package com.nimbusphagia.library.loan.internal.service;

import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.catalog.CatalogApi;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanDetailResponse;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanFilter;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanLineDetailResponse;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanSummaryResponse;
import com.nimbusphagia.library.loan.internal.dto.visitor.VisitorResponse;
import com.nimbusphagia.library.loan.internal.entity.LoanMovement;
import com.nimbusphagia.library.loan.internal.entity.LoanMovementLine;
import com.nimbusphagia.library.loan.internal.entity.LoanStatus;
import com.nimbusphagia.library.loan.internal.repository.LoanMovementLineRepository;
import com.nimbusphagia.library.loan.internal.repository.LoanMovementLineRepository.LineCount;
import com.nimbusphagia.library.loan.internal.repository.LoanMovementRepository;
import com.nimbusphagia.library.loan.internal.repository.LoanSpecifications;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanQueryService {
  private final LoanMovementRepository movementRepository;
  private final LoanMovementLineRepository lineRepository;
  private final CatalogApi catalogApi;
  private final Clock clock;

  @Transactional(readOnly = true)
  public Page<LoanSummaryResponse> findAll(Long libraryId, LoanFilter filter, Pageable pageable) {
    if (filter.createdFrom() != null
        && filter.createdTo() != null
        && filter.createdFrom().isAfter(filter.createdTo())) {
      throw new BadRequestException("createdFrom must not be after createdTo");
    }
    Instant now = clock.instant();

    Page<LoanMovement> page =
        movementRepository.findAll(LoanSpecifications.from(libraryId, filter, now), pageable);

    Map<Long, LineCount> counts = new HashMap<>();
    if (page.hasContent()) {
      List<Long> ids = page.getContent().stream().map(LoanMovement::getId).toList();
      lineRepository.countByMovementIds(ids).forEach(c -> counts.put(c.getMovementId(), c));
    }

    return page.map(
        m -> {
          LineCount c = counts.get(m.getId());
          long total = c == null ? 0L : c.getTotal();
          long open = c == null ? 0L : c.getOpenCount();
          return LoanSummaryResponse.fromEntity(m, total, open, now);
        });
  }

  @Transactional(readOnly = true)
  public LoanDetailResponse findOne(Long libraryId, Long loanId) {
    LoanMovement movement =
        movementRepository
            .findDetailedById(loanId)
            .filter(m -> m.getLibrary().getId().equals(libraryId))
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found in this library"));

    List<LoanMovementLine> lines = lineRepository.findByMovementIdOrderByIdAsc(loanId);

    Set<Long> bookIds =
        lines.stream().map(l -> l.getCopy().getBook().getId()).collect(Collectors.toSet());
    Map<Long, BookLabelData> books = catalogApi.getBookLabelData(bookIds);

    List<LoanLineDetailResponse> lineResponses =
        lines.stream()
            .map(
                l -> {
                  Copy copy = l.getCopy();
                  Long bookId = copy.getBook().getId();
                  BookLabelData book = books.get(bookId);
                  return new LoanLineDetailResponse(
                      l.getId(),
                      copy.getId(),
                      bookId,
                      book == null ? null : book.title(),
                      copy.getIteration(),
                      copy.getCondition(),
                      l.getResolvedAt(),
                      l.getResolvedBy() == null ? null : l.getResolvedBy().getId(),
                      l.getResolvedBy() == null ? null : l.getResolvedBy().getFullName(),
                      l.getOutcome());
                })
            .toList();

    long openLines = lines.stream().filter(l -> l.getResolvedAt() == null).count();

    return new LoanDetailResponse(
        movement.getId(),
        libraryId,
        VisitorResponse.fromEntity(movement.getLoanedTo()),
        movement.getLoanedBy().getId(),
        movement.getLoanedBy().getFullName(),
        movement.getCreatedAt(),
        movement.getExpiresBy(),
        LoanStatus.of(openLines, movement.getExpiresBy(), clock.instant()),
        movement.getNotes(),
        lineResponses);
  }
}
