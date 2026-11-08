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
import com.nimbusphagia.library.inventory.internal.dto.stock.StockExitCopyResponse;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockExitRequest;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockExitResponse;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockMovementService {
  private static final Set<MovementReason> ENTRY_REASONS =
      EnumSet.of(MovementReason.PURCHASE, MovementReason.DONATION, MovementReason.OTHER);

  private static final Set<MovementReason> EXIT_REASONS =
      EnumSet.of(
          MovementReason.LOSS,
          MovementReason.DAMAGE,
          MovementReason.DISCARD,
          MovementReason.TRANSFER,
          MovementReason.OTHER);

  private final LibraryRepository libraryRepository;
  private final ShelfRepository shelfRepository;
  private final CopyRepository copyRepository;
  private final StockMovementRepository movementRepository;
  private final StockMovementLineRepository lineRepository;
  private final CatalogApi catalogApi;
  private final LabelService labelService;
  private final EntityManager entityManager;

  // Entry Movements
  @Transactional
  public StockEntryResponse registerEntry(Long libraryId, StockEntryRequest request, Long actorId) {
    if (!ENTRY_REASONS.contains(request.reason())) {
      throw new BadRequestException(request.reason() + " is not a valid reason for an entry");
    }
    Library library = activeLibrary(libraryId);
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

  // Exit Movements
  @Transactional
  public StockExitResponse registerExit(Long libraryId, StockExitRequest request, Long actorId) {
    if (!EXIT_REASONS.contains(request.reason())) {
      throw new BadRequestException(request.reason() + " is not a valid reason for an exit");
    }
    return exit(
        activeLibrary(libraryId),
        request.reason(),
        statusFor(request.reason()),
        request.notes(),
        request.copyIds(),
        actorId,
        false);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public StockExitResponse registerLoanLoss(
      Long libraryId, List<Long> copyIds, String notes, Long actorId) {
    Library library =
        libraryRepository
            .findById(libraryId)
            .orElseThrow(() -> new ResourceNotFoundException("Library not found"));
    String cleanNotes = notes == null || notes.isBlank() ? null : notes.strip();
    return exit(library, MovementReason.LOSS, CopyStatus.LOST, cleanNotes, copyIds, actorId, true);
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

  private Library activeLibrary(Long libraryId) {
    Library library =
        libraryRepository
            .findById(libraryId)
            .orElseThrow(() -> new ResourceNotFoundException("Library not found"));
    if (!library.isActive()) {
      throw new BadRequestException("This library is inactive");
    }
    return library;
  }

  private static CopyStatus statusFor(MovementReason reason) {
    return switch (reason) {
      case LOSS -> CopyStatus.LOST;
      case TRANSFER -> CopyStatus.TRANSFERRED;
      case DAMAGE, DISCARD, OTHER -> CopyStatus.DISCARDED;
      default -> throw new BadRequestException(reason + " is not a valid reason for an exit");
    };
  }

  private static void validateTransition(Copy copy, CopyStatus target, boolean fromLoan) {
    CopyStatus current = copy.getStatus();
    boolean ok =
        fromLoan
            ? current == CopyStatus.ON_LOAN
            : current == CopyStatus.AVAILABLE
                || (current == CopyStatus.LOST && target == CopyStatus.DISCARDED);
    if (!ok) {
      throw new BadRequestException(
          "Copy " + copy.getId() + " is " + current + " and cannot become " + target);
    }
  }

  private StockExitResponse exit(
      Library library,
      MovementReason reason,
      CopyStatus target,
      String notes,
      List<Long> copyIds,
      Long actorId,
      boolean fromLoan) {
    Long libraryId = library.getId();

    Set<Long> ids = new HashSet<>();
    for (Long id : copyIds) {
      if (!ids.add(id)) {
        throw new BadRequestException("Copy " + id + " appears more than once");
      }
    }

    Map<Long, Copy> byId = new HashMap<>();
    for (Copy copy : copyRepository.findAllByIdInForUpdate(ids)) {
      byId.put(copy.getId(), copy);
    }

    List<Copy> copies = new ArrayList<>();
    for (Long id : copyIds) {
      Copy copy = byId.get(id);
      if (copy == null || !copy.getLibrary().getId().equals(libraryId)) {
        throw new ResourceNotFoundException("Copy " + id + " not found in this library");
      }
      validateTransition(copy, target, fromLoan);
      copies.add(copy);
    }

    StockMovement movement =
        movementRepository.save(
            StockMovement.builder()
                .library(library)
                .type(MovementType.EXIT)
                .reason(reason)
                .signedBy(entityManager.getReference(Account.class, actorId))
                .notes(notes)
                .build());

    lineRepository.saveAll(
        copies.stream()
            .map(c -> StockMovementLine.builder().movement(movement).copy(c).build())
            .toList());

    List<StockExitCopyResponse> responses = new ArrayList<>();
    for (Copy copy : copies) {
      copy.setStatus(target);
      labelService.retire(copy.getId());
      responses.add(
          new StockExitCopyResponse(
              copy.getId(), copy.getBook().getId(), copy.getIteration(), target));
    }
    copyRepository.saveAllAndFlush(copies);

    return new StockExitResponse(
        movement.getId(),
        libraryId,
        movement.getReason(),
        movement.getNotes(),
        movement.getCreatedAt(),
        responses);
  }
}
