package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.catalog.CatalogApi;
import com.nimbusphagia.library.catalog.internal.entity.Book;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelResponse;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockEntryCopyResponse;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockEntryItemRequest;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockEntryRequest;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockEntryResponse;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;
import com.nimbusphagia.library.inventory.internal.entity.Library;
import com.nimbusphagia.library.inventory.internal.entity.MovementReason;
import com.nimbusphagia.library.inventory.internal.entity.MovementType;
import com.nimbusphagia.library.inventory.internal.entity.Shelf;
import com.nimbusphagia.library.inventory.internal.entity.StockMovement;
import com.nimbusphagia.library.inventory.internal.entity.StockMovementLine;
import com.nimbusphagia.library.inventory.internal.repository.CopyRepository;
import com.nimbusphagia.library.inventory.internal.repository.LibraryRepository;
import com.nimbusphagia.library.inventory.internal.repository.ShelfRepository;
import com.nimbusphagia.library.inventory.internal.repository.StockMovementLineRepository;
import com.nimbusphagia.library.inventory.internal.repository.StockMovementRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockMovementService {
  private static final Set<MovementReason> ENTRY_REASONS =
      EnumSet.of(MovementReason.PURCHASE, MovementReason.DONATION, MovementReason.OTHER);

  private final LibraryRepository libraryRepository;
  private final ShelfRepository shelfRepository;
  private final CopyRepository copyRepository;
  private final StockMovementRepository movementRepository;
  private final StockMovementLineRepository lineRepository;
  private final CatalogApi catalogApi;
  private final LabelService labelService;
  private final EntityManager entityManager;

  @Transactional
  public StockEntryResponse registerEntry(Long libraryId, StockEntryRequest request, Long actorId) {
    if (!ENTRY_REASONS.contains(request.reason())) {
      throw new BadRequestException(request.reason() + " is not a valid reason for an entry");
    }
    Library library =
        libraryRepository
            .findById(libraryId)
            .orElseThrow(() -> new ResourceNotFoundException("Library not found"));
    if (!library.isActive()) {
      throw new BadRequestException("This library is inactive");
    }

    validateBooks(request.items());

    StockMovement movement =
        movementRepository.save(
            StockMovement.builder()
                .library(library)
                .type(MovementType.ENTRY)
                .reason(request.reason())
                .signedBy(entityManager.getReference(Account.class, actorId))
                .notes(request.notes())
                .build());

    Map<Long, Shelf> shelfCache = new HashMap<>();
    List<StockEntryCopyResponse> responses = new ArrayList<>();

    for (StockEntryItemRequest item : request.items()) {
      Shelf shelf = resolveShelf(libraryId, item.shelfId(), shelfCache);
      Book book = entityManager.getReference(Book.class, item.bookId());

      List<Copy> copies = createCopies(library, book, shelf, item);

      lineRepository.saveAll(
          copies.stream()
              .map(c -> StockMovementLine.builder().movement(movement).copy(c).build())
              .toList());

      for (Copy copy : copies) {
        LabelResponse label = item.labelParts() == null ? null : createLabel(copy, item, actorId);
        responses.add(
            new StockEntryCopyResponse(
                copy.getId(),
                item.bookId(),
                copy.getIteration(),
                shelf.getId(),
                shelf.getName(),
                label == null ? null : label.id(),
                label == null ? null : label.text()));
      }
    }

    return new StockEntryResponse(
        movement.getId(),
        libraryId,
        movement.getReason(),
        movement.getNotes(),
        movement.getCreatedAt(),
        responses);
  }

  // Helpers
  private void validateBooks(List<StockEntryItemRequest> items) {
    Set<Long> bookIds = new HashSet<>();
    for (StockEntryItemRequest item : items) {
      if (!bookIds.add(item.bookId())) {
        throw new BadRequestException(
            "Book " + item.bookId() + " appears more than once; merge it into a single item");
      }
    }
    Map<Long, BookLabelData> found = catalogApi.getBookLabelData(bookIds);
    for (Long id : bookIds) {
      if (!found.containsKey(id)) {
        throw new ResourceNotFoundException("Book " + id + " not found");
      }
    }
  }

  private Shelf resolveShelf(Long libraryId, Long shelfId, Map<Long, Shelf> cache) {
    return cache.computeIfAbsent(
        shelfId,
        id ->
            id != null
                ? shelfRepository
                    .findByIdAndLibraryId(id, libraryId)
                    .orElseThrow(
                        () ->
                            new BadRequestException(
                                "Shelf " + id + " does not belong to this library"))
                : shelfRepository
                    .findByLibraryIdAndDefaultShelfTrue(libraryId)
                    .orElseThrow(
                        () ->
                            new BadRequestException(
                                "This library has no default shelf; specify a shelfId")));
  }

  private List<Copy> createCopies(
      Library library, Book book, Shelf shelf, StockEntryItemRequest item) {
    int first =
        copyRepository.findMaxHeldIteration(
                library.getId(), item.bookId(), CopyStatus.numberHolding())
            + 1;

    List<Copy> copies = new ArrayList<>();
    for (int i = 0; i < item.quantity(); i++) {
      copies.add(
          Copy.builder().library(library).book(book).shelf(shelf).iteration(first + i).build());
    }
    try {
      return copyRepository.saveAllAndFlush(copies);
    } catch (DataIntegrityViolationException e) {
      String cause = String.valueOf(NestedExceptionUtils.getMostSpecificCause(e).getMessage());
      if (cause.contains("uq_copy_library_book_iteration")) {
        throw new ConflictException(
            "Copy numbers for book " + item.bookId() + " changed while registering; please retry");
      }
      throw e;
    }
  }

  private LabelResponse createLabel(Copy copy, StockEntryItemRequest item, Long actorId) {
    try {
      return labelService.createFor(copy, item.labelParts(), actorId);
    } catch (ConflictException e) {
      throw new ConflictException(
          "Copy c." + copy.getIteration() + " of book " + item.bookId() + ": " + e.getMessage());
    }
  }
}
