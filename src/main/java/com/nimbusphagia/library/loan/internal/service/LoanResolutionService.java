package com.nimbusphagia.library.loan.internal.service;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.exception.ErrorCodes;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ForbiddenException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.InventoryApi;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanDetailResponse;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanResolutionRequest;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanResolutionRequest.LineResolution;
import com.nimbusphagia.library.loan.internal.entity.LoanMovement;
import com.nimbusphagia.library.loan.internal.entity.LoanMovementLine;
import com.nimbusphagia.library.loan.internal.entity.LoanOutcome;
import com.nimbusphagia.library.loan.internal.repository.LoanMovementLineRepository;
import com.nimbusphagia.library.loan.internal.repository.LoanMovementRepository;
import com.nimbusphagia.library.organization.AccessGuard;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanResolutionService {
  private final LoanMovementRepository movementRepository;
  private final LoanMovementLineRepository lineRepository;
  private final LoanQueryService queryService;
  private final InventoryApi inventoryApi;
  private final AccessGuard accessGuard;
  private final EntityManager entityManager;
  private final Clock clock;

  @Transactional
  public LoanDetailResponse resolve(
      Long libraryId, Long loanId, LoanResolutionRequest request, Long actorId) {
    Map<Long, LoanOutcome> outcomes = new LinkedHashMap<>();
    for (LineResolution r : request.lines()) {
      if (outcomes.put(r.lineId(), r.outcome()) != null) {
        throw new BadRequestException("Line " + r.lineId() + " appears more than once");
      }
    }
    // Both outcomes take the copy out of stock, which is an exit
    if ((outcomes.containsValue(LoanOutcome.LOST) || outcomes.containsValue(LoanOutcome.DISCARDED))
        && !accessGuard.hasRole("LIBRARIAN")) {
      throw new ForbiddenException(
          ErrorCodes.REQUIRES_LIBRARIAN, "Only a librarian can resolve a loan as lost or discarded");
    }

    LoanMovement movement =
        movementRepository
            .findById(loanId)
            .filter(m -> m.getLibrary().getId().equals(libraryId))
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found in this library"));

    Map<Long, LoanMovementLine> byId = new HashMap<>();
    for (LoanMovementLine line :
        lineRepository.findByMovementIdAndIdInForUpdate(movement.getId(), outcomes.keySet())) {
      byId.put(line.getId(), line);
    }

    Set<Long> returned = new HashSet<>();
    Set<Long> returnedDamaged = new HashSet<>();
    Map<Long, Long> lost = new HashMap<>();
    Map<Long, Long> discarded = new HashMap<>();

    for (Map.Entry<Long, LoanOutcome> e : outcomes.entrySet()) {
      LoanMovementLine line = byId.get(e.getKey());
      if (line == null) {
        throw new ResourceNotFoundException("Line " + e.getKey() + " not found in this loan");
      }
      if (line.getResolvedAt() != null) {
        throw new ConflictException("Line " + e.getKey() + " is already resolved");
      }
      Long copyId = line.getCopy().getId();
      switch (e.getValue()) {
        case RETURNED -> returned.add(copyId);
        case RETURNED_DAMAGED -> returnedDamaged.add(copyId);
        case LOST -> lost.put(copyId, line.getId());
        case DISCARDED -> discarded.put(copyId, line.getId());
      }
    }

    inventoryApi.releaseFromLoan(returned, false, actorId, request.notes());
    inventoryApi.releaseFromLoan(returnedDamaged, true, actorId, request.notes());
    inventoryApi.registerLoanLoss(libraryId, lost, actorId, request.notes());
    inventoryApi.registerLoanDiscard(libraryId, discarded, actorId, request.notes());

    Instant now = clock.instant();
    Account actor = entityManager.getReference(Account.class, actorId);
    for (LoanMovementLine line : byId.values()) {
      line.setResolvedAt(now);
      line.setResolvedBy(actor);
      line.setOutcome(outcomes.get(line.getId()));
    }
    lineRepository.saveAllAndFlush(byId.values());

    return queryService.findOne(libraryId, loanId);
  }
}
