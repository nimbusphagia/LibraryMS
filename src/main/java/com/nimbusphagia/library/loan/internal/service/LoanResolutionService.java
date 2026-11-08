package com.nimbusphagia.library.loan.internal.service;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
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
    Set<Long> lost = new HashSet<>();

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
        case LOST -> lost.add(copyId);
      }
    }

    inventoryApi.releaseFromLoan(returned, false);
    inventoryApi.releaseFromLoan(returnedDamaged, true);
    inventoryApi.registerLoanLoss(libraryId, lost, actorId, request.notes());

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
