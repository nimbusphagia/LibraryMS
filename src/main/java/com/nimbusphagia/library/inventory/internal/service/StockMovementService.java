package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.account.internal.entity.Account;
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
import com.nimbusphagia.library.inventory.internal.dto.stock.StockRecoveryRequest;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockTransferRequest;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockTransferResponse;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockTransferResponse.TransferredCopy;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;
import com.nimbusphagia.library.inventory.internal.entity.Label;
import com.nimbusphagia.library.inventory.internal.entity.Library;
import com.nimbusphagia.library.inventory.internal.entity.MovementReason;
import com.nimbusphagia.library.inventory.internal.entity.MovementType;
import com.nimbusphagia.library.inventory.internal.entity.Shelf;
import com.nimbusphagia.library.inventory.internal.entity.StockMovement;
import com.nimbusphagia.library.inventory.internal.entity.StockMovementLine;
import com.nimbusphagia.library.inventory.internal.repository.CopyRepository;
import com.nimbusphagia.library.inventory.internal.repository.LabelRepository;
import com.nimbusphagia.library.inventory.internal.repository.LibraryRepository;
import com.nimbusphagia.library.inventory.internal.repository.ShelfRepository;
import com.nimbusphagia.library.inventory.internal.repository.StockMovementLineRepository;
import com.nimbusphagia.library.inventory.internal.repository.StockMovementRepository;
import com.nimbusphagia.library.organization.AccessGuard;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

  // Transfers have their own operation, and loans exit copies with LOSS or DAMAGE
  private static final Set<MovementReason> EXIT_REASONS =
      EnumSet.of(
          MovementReason.LOSS, MovementReason.DAMAGE, MovementReason.DISCARD, MovementReason.OTHER);

  private final LibraryRepository libraryRepository;
  private final ShelfRepository shelfRepository;
  private final CopyRepository copyRepository;
  private final StockMovementRepository movementRepository;
  private final StockMovementLineRepository lineRepository;
  private final LabelRepository labelRepository;
  private final CatalogApi catalogApi;
  private final LabelService labelService;
  private final AccessGuard accessGuard;
  private final EntityManager entityManager;

  // Entry Movements
  @Transactional
  public StockEntryResponse registerEntry(Long libraryId, StockEntryRequest request, Long actorId) {
    if (!ENTRY_REASONS.contains(request.reason())) {
      throw new BadRequestException(request.reason() + " is not a valid reason for an entry");
    }
    Library library = activeLibrary(libraryId);
    validateBooks(library.getInstitutionId(), request.items());

    StockMovement movement =
        movement(library, MovementType.ENTRY, request.reason(), request.notes(), actorId);

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

    return entryResponse(movement, responses);
  }

  @Transactional
  public StockEntryResponse registerRecovery(
      Long libraryId, StockRecoveryRequest request, Long actorId) {
    Library library = activeLibrary(libraryId);
    List<Copy> copies = lockInLibrary(libraryId, request.copyIds());
    Map<Long, StockMovementLine> lossLines = new HashMap<>();
    for (Copy copy : copies) {
      if (copy.getStatus() != CopyStatus.LOST) {
        throw new BadRequestException(
            "Copy " + copy.getId() + " is " + copy.getStatus() + "; only lost copies are recovered");
      }
      lossLines.put(
          copy.getId(),
          lineRepository
              .findFirstByCopyIdAndMovementReasonOrderByIdDesc(copy.getId(), MovementReason.LOSS)
              .orElseThrow(
                  () -> new BadRequestException("Copy " + copy.getId() + " has no loss to undo")));
    }

    StockMovement movement =
        movement(library, MovementType.ENTRY, MovementReason.RECOVERY, request.notes(), actorId);

    for (Copy copy : copies) {
      // The number may have gone to a new copy of the same book while this one was lost
      if (copyRepository.existsByLibraryIdAndBookIdAndIterationAndStatusIn(
          libraryId, copy.getBook().getId(), copy.getIteration(), CopyStatus.numberHolding())) {
        copy.setIteration(nextIteration(libraryId, copy.getBook().getId()));
      }
      copy.setStatus(CopyStatus.AVAILABLE);
      copyRepository.saveAndFlush(copy);
      labelService.reactivate(copy);
    }
    lineRepository.saveAll(
        copies.stream()
            .map(
                c ->
                    StockMovementLine.builder()
                        .movement(movement)
                        .copy(c)
                        .recoveredLine(lossLines.get(c.getId()))
                        .build())
            .toList());

    Map<Long, Label> labels =
        labelRepository
            .findByCopyIdInAndRetiredAtIsNull(copies.stream().map(Copy::getId).toList())
            .stream()
            .collect(Collectors.toMap(l -> l.getCopy().getId(), l -> l));
    return entryResponse(
        movement,
        copies.stream()
            .map(
                c -> {
                  Label label = labels.get(c.getId());
                  return new StockEntryCopyResponse(
                      c.getId(),
                      c.getBook().getId(),
                      c.getIteration(),
                      c.getShelf().getId(),
                      c.getShelf().getName(),
                      label == null ? null : label.getId(),
                      label == null ? null : label.getText());
                })
            .toList());
  }

  // Exit Movements
  @Transactional
  public StockExitResponse registerExit(Long libraryId, StockExitRequest request, Long actorId) {
    if (!EXIT_REASONS.contains(request.reason())) {
      throw new BadRequestException(request.reason() + " is not a valid reason for an exit");
    }
    Library library = activeLibrary(libraryId);
    CopyStatus target = statusFor(request.reason());
    List<Copy> copies = lockInLibrary(libraryId, request.copyIds());
    StockMovement movement =
        exit(library, request.reason(), target, request.notes(), copies, Map.of(), actorId);

    return new StockExitResponse(
        movement.getId(),
        libraryId,
        movement.getReason(),
        movement.getNotes(),
        movement.getCreatedAt(),
        copies.stream()
            .map(
                c ->
                    new StockExitCopyResponse(
                        c.getId(), c.getBook().getId(), c.getIteration(), target))
            .toList());
  }

  // Keys are copy ids, values the loan lines whose outcome takes the copy out of stock
  @Transactional(propagation = Propagation.MANDATORY)
  public void registerLoanExit(
      Long libraryId,
      MovementReason reason,
      Map<Long, Long> loanLineByCopy,
      String notes,
      Long actorId) {
    Library library =
        libraryRepository
            .findById(libraryId)
            .orElseThrow(() -> new ResourceNotFoundException("Library not found"));
    List<Copy> copies = lockInLibrary(libraryId, List.copyOf(loanLineByCopy.keySet()));
    exit(library, reason, statusFor(reason), notes, copies, loanLineByCopy, actorId);
  }

  // Transfers
  @Transactional
  public StockTransferResponse registerTransfer(
      Long libraryId, StockTransferRequest request, Long actorId) {
    Long destinationId = request.destinationLibraryId();
    if (destinationId.equals(libraryId)) {
      throw new BadRequestException("A copy cannot be transferred to its own library");
    }
    accessGuard.requireLibrary(destinationId);
    Library origin = activeLibrary(libraryId);
    Library destination = activeLibrary(destinationId);
    if (!origin.getInstitutionId().equals(destination.getInstitutionId())) {
      throw new BadRequestException("Copies are only transferred within the same institution");
    }
    Shelf shelf =
        shelfRepository
            .findByLibraryIdAndDefaultShelfTrue(destinationId)
            .orElseThrow(
                () -> new BadRequestException("The destination library has no default shelf"));

    List<Copy> originals = lockInLibrary(libraryId, request.copyIds());
    StockMovement exit =
        exit(
            origin,
            MovementReason.TRANSFER,
            CopyStatus.TRANSFERRED,
            request.notes(),
            originals,
            Map.of(),
            actorId);

    Map<Long, Integer> nextByBook = new HashMap<>();
    List<Copy> transferred = new ArrayList<>();
    for (Copy original : originals) {
      int iteration =
          nextByBook.compute(
              original.getBook().getId(),
              (book, last) -> last == null ? nextIteration(destinationId, book) : last + 1);
      transferred.add(
          Copy.builder()
              .library(destination)
              .book(original.getBook())
              .shelf(shelf)
              .iteration(iteration)
              .damagedAt(original.getDamagedAt())
              .damagedBy(original.getDamagedBy())
              .transferredFrom(original)
              .build());
    }
    copyRepository.saveAllAndFlush(transferred);

    StockMovement entry =
        movement(
            destination, MovementType.ENTRY, MovementReason.TRANSFER, request.notes(), actorId);
    lineRepository.saveAll(
        transferred.stream()
            .map(c -> StockMovementLine.builder().movement(entry).copy(c).build())
            .toList());

    return new StockTransferResponse(
        exit.getId(),
        entry.getId(),
        destinationId,
        transferred.stream()
            .map(
                c ->
                    new TransferredCopy(
                        c.getTransferredFrom().getId(),
                        c.getId(),
                        c.getBook().getId(),
                        c.getIteration()))
            .toList());
  }

  // Helpers
  private void validateBooks(Long institutionId, List<StockEntryItemRequest> items) {
    Set<Long> bookIds = new HashSet<>();
    for (StockEntryItemRequest item : items) {
      if (!bookIds.add(item.bookId())) {
        throw new BadRequestException(
            "Book " + item.bookId() + " appears more than once; merge it into a single item");
      }
    }
    Set<Long> usable = catalogApi.findUsableBookIds(institutionId, bookIds);
    for (Long id : bookIds) {
      if (!usable.contains(id)) {
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

  private int nextIteration(Long libraryId, Long bookId) {
    return copyRepository.findMaxHeldIteration(libraryId, bookId, CopyStatus.numberHolding()) + 1;
  }

  private List<Copy> createCopies(
      Library library, Book book, Shelf shelf, StockEntryItemRequest item) {
    int first = nextIteration(library.getId(), item.bookId());

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

  // Copies in request order, locked for update
  private List<Copy> lockInLibrary(Long libraryId, List<Long> copyIds) {
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
      copies.add(copy);
    }
    return copies;
  }

  private StockMovement movement(
      Library library, MovementType type, MovementReason reason, String notes, Long actorId) {
    return movementRepository.save(
        StockMovement.builder()
            .library(library)
            .type(type)
            .reason(reason)
            .signedBy(entityManager.getReference(Account.class, actorId))
            .notes(notes)
            .build());
  }

  private StockMovement exit(
      Library library,
      MovementReason reason,
      CopyStatus target,
      String notes,
      List<Copy> copies,
      Map<Long, Long> loanLineByCopy,
      Long actorId) {
    for (Copy copy : copies) {
      CopyStatus current = copy.getStatus();
      boolean ok =
          loanLineByCopy.isEmpty()
              ? current == CopyStatus.AVAILABLE
                  || (current == CopyStatus.LOST && target == CopyStatus.DISCARDED)
              : current == CopyStatus.ON_LOAN;
      if (!ok) {
        throw new BadRequestException(
            "Copy " + copy.getId() + " is " + current + " and cannot become " + target);
      }
    }

    StockMovement movement = movement(library, MovementType.EXIT, reason, notes, actorId);
    lineRepository.saveAll(
        copies.stream()
            .map(
                c ->
                    StockMovementLine.builder()
                        .movement(movement)
                        .copy(c)
                        .loanLineId(loanLineByCopy.get(c.getId()))
                        .build())
            .toList());

    for (Copy copy : copies) {
      copy.setStatus(target);
      labelService.retire(copy.getId());
    }
    copyRepository.saveAllAndFlush(copies);
    return movement;
  }

  private static StockEntryResponse entryResponse(
      StockMovement movement, List<StockEntryCopyResponse> copies) {
    return new StockEntryResponse(
        movement.getId(),
        movement.getLibrary().getId(),
        movement.getReason(),
        movement.getNotes(),
        movement.getCreatedAt(),
        copies);
  }
}
