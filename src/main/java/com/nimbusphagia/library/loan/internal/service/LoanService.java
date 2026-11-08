package com.nimbusphagia.library.loan.internal.service;

import static com.nimbusphagia.library.loan.internal.util.LoanTime.LIMA;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.InventoryApi;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.Library;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanRequest;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanResponse;
import com.nimbusphagia.library.loan.internal.entity.LoanMovement;
import com.nimbusphagia.library.loan.internal.entity.LoanMovementLine;
import com.nimbusphagia.library.loan.internal.entity.Visitor;
import com.nimbusphagia.library.loan.internal.entity.VisitorRole;
import com.nimbusphagia.library.loan.internal.repository.LoanMovementLineRepository;
import com.nimbusphagia.library.loan.internal.repository.LoanMovementRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanService {

  private final VisitorRepository visitorRepository;
  private final LoanMovementRepository movementRepository;
  private final LoanMovementLineRepository lineRepository;
  private final InventoryApi inventoryApi;
  private final EntityManager entityManager;
  private final Clock clock;

  @Transactional
  public LoanResponse create(Long libraryId, LoanRequest request, Long actorId) {
    Set<Long> copyIds = new LinkedHashSet<>(request.copyIds());
    if (copyIds.size() != request.copyIds().size()) {
      throw new BadRequestException("The same copy appears more than once in the request");
    }

    Visitor visitor =
        visitorRepository
            .findByIdForUpdate(request.visitorId())
            .orElseThrow(() -> new ResourceNotFoundException("Visitor not found"));

    if (!Boolean.TRUE.equals(visitor.getActive())) {
      throw new BadRequestException("This visitor is inactive");
    }
    VisitorRole role = visitor.getRole();
    long open = lineRepository.countOpenByVisitorIdAndLibraryId(visitor.getId(), libraryId);
    if (open + copyIds.size() > role.getMaxActiveLoans()) {
      throw new ConflictException(
          "Loan limit exceeded: the visitor has "
              + open
              + " open loan(s) in this library and the maximum is "
              + role.getMaxActiveLoans());
    }

    Instant expiresBy = endOfDayInLima(role.getMaxDays());

    inventoryApi.reserveForLoan(libraryId, copyIds);

    LoanMovement movement =
        movementRepository.save(
            LoanMovement.builder()
                .library(entityManager.getReference(Library.class, libraryId))
                .loanedBy(entityManager.getReference(Account.class, actorId))
                .loanedTo(visitor)
                .expiresBy(expiresBy)
                .notes(request.notes())
                .build());

    List<LoanMovementLine> lines =
        lineRepository.saveAll(
            copyIds.stream()
                .map(
                    id ->
                        LoanMovementLine.builder()
                            .movement(movement)
                            .copy(entityManager.getReference(Copy.class, id))
                            .build())
                .toList());

    return LoanResponse.fromEntity(movement, lines);
  }

  // Helpers
  private Instant endOfDayInLima(int maxDays) {
    LocalDate lastDay = LocalDate.now(clock.withZone(LIMA)).plusDays(maxDays);
    return lastDay.plusDays(1).atStartOfDay(LIMA).toInstant().minus(1, ChronoUnit.MICROS);
  }
}
