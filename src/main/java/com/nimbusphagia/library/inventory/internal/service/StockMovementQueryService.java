package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.catalog.CatalogApi;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockMovementDetailResponse;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockMovementFilter;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockMovementLineResponse;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockMovementSummaryResponse;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.StockMovement;
import com.nimbusphagia.library.inventory.internal.entity.StockMovementLine;
import com.nimbusphagia.library.inventory.internal.repository.LibraryRepository;
import com.nimbusphagia.library.inventory.internal.repository.StockMovementLineRepository;
import com.nimbusphagia.library.inventory.internal.repository.StockMovementRepository;
import com.nimbusphagia.library.inventory.internal.repository.StockMovementSpecifications;
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
public class StockMovementQueryService {
  private final LibraryRepository libraryRepository;
  private final StockMovementRepository movementRepository;
  private final StockMovementLineRepository lineRepository;
  private final CatalogApi catalogApi;

  @Transactional(readOnly = true)
  public Page<StockMovementSummaryResponse> findAll(
      Long libraryId, StockMovementFilter filter, Pageable pageable) {
    if (!libraryRepository.existsById(libraryId)) {
      throw new ResourceNotFoundException("Library not found");
    }

    Page<StockMovement> page =
        movementRepository.findAll(StockMovementSpecifications.from(libraryId, filter), pageable);

    Map<Long, Long> counts = new HashMap<>();
    if (page.hasContent()) {
      List<Long> ids = page.getContent().stream().map(StockMovement::getId).toList();
      lineRepository
          .countByMovementIds(ids)
          .forEach(c -> counts.put(c.getMovementId(), c.getTotal()));
    }

    return page.map(
        m -> StockMovementSummaryResponse.fromEntity(m, counts.getOrDefault(m.getId(), 0L)));
  }

  @Transactional(readOnly = true)
  public StockMovementDetailResponse findOne(Long libraryId, Long movementId) {
    StockMovement movement =
        movementRepository
            .findDetailedById(movementId)
            .filter(m -> m.getLibrary().getId().equals(libraryId))
            .orElseThrow(() -> new ResourceNotFoundException("Movement not found in this library"));

    List<StockMovementLine> lines =
        lineRepository.findByMovementIdOrderByCopyBookIdAscCopyIterationAsc(movementId);

    Set<Long> bookIds =
        lines.stream().map(l -> l.getCopy().getBook().getId()).collect(Collectors.toSet());
    Map<Long, BookLabelData> books = catalogApi.getBookLabelData(bookIds);

    List<StockMovementLineResponse> copies =
        lines.stream()
            .map(
                l -> {
                  Copy copy = l.getCopy();
                  Long bookId = copy.getBook().getId();
                  BookLabelData book = books.get(bookId);
                  return new StockMovementLineResponse(
                      copy.getId(),
                      bookId,
                      book == null ? null : book.title(),
                      copy.getIteration(),
                      copy.getShelf().getId(),
                      copy.getShelf().getName(),
                      copy.getStatus());
                })
            .toList();

    return new StockMovementDetailResponse(
        movement.getId(),
        libraryId,
        movement.getType(),
        movement.getReason(),
        movement.getNotes(),
        movement.getSignedBy().getId(),
        movement.getSignedBy().getFullName(),
        movement.getCreatedAt(),
        copies);
  }
}
