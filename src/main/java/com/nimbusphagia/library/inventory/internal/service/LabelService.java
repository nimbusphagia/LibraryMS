package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.catalog.CatalogApi;
import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.exception.ErrorCodes;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ForbiddenException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelCustomizeRequest;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelRevisionResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestion;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestion.PartSuggestion;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;
import com.nimbusphagia.library.inventory.internal.entity.Label;
import com.nimbusphagia.library.inventory.internal.entity.LabelLayout;
import com.nimbusphagia.library.inventory.internal.entity.LabelLayout.PartRule;
import com.nimbusphagia.library.inventory.internal.entity.LabelOrigin;
import com.nimbusphagia.library.inventory.internal.entity.LabelRevision;
import com.nimbusphagia.library.inventory.internal.entity.LabelScheme;
import com.nimbusphagia.library.inventory.internal.entity.LabelValue;
import com.nimbusphagia.library.inventory.internal.entity.PrintStatus;
import com.nimbusphagia.library.inventory.internal.repository.CopyRepository;
import com.nimbusphagia.library.inventory.internal.repository.LabelRepository;
import com.nimbusphagia.library.inventory.internal.repository.LabelRevisionRepository;
import com.nimbusphagia.library.inventory.internal.repository.LibraryRepository;
import com.nimbusphagia.library.inventory.internal.service.LabelDeriver.Options;
import com.nimbusphagia.library.inventory.internal.util.LabelTextComposer;
import com.nimbusphagia.library.organization.AccessGuard;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
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
public class LabelService {
  private static final String INITIAL_REASON = "Initial label";
  private static final String RECOVERED_REASON = "Recovered";

  private final LabelRepository labelRepository;
  private final LabelRevisionRepository revisionRepository;
  private final CopyRepository copyRepository;
  private final LibraryRepository libraryRepository;
  private final CatalogApi catalogApi;
  private final LabelDeriver deriver;
  private final AccessGuard accessGuard;
  private final EntityManager entityManager;

  @Transactional(readOnly = true)
  public LabelSuggestion suggest(Long libraryId, Long bookId, Integer copyNumber) {
    if (copyNumber != null && copyNumber < 1) {
      throw new BadRequestException("Copy number must be positive");
    }
    int number =
        copyNumber != null
            ? copyNumber
            : copyRepository.findMaxHeldIteration(libraryId, bookId, CopyStatus.numberHolding())
                + 1;
    LabelScheme scheme = libraryRepository.findById(libraryId).orElseThrow().getLabelScheme();
    Map<LabelPart, Options> options = deriver.derive(catalogApi.getBookLabelData(bookId));
    Map<LabelPart, LabelValue> kept = keptManualValues(libraryId, bookId);

    List<PartSuggestion> parts = new ArrayList<>();
    List<LabelPart> missing = new ArrayList<>();
    Map<LabelPart, String> values = new EnumMap<>(LabelPart.class);
    for (PartRule rule : writableRules(scheme.getLayout())) {
      Options o = options.get(rule.part());
      LabelValue value = defaultValue(o, kept.get(rule.part()));
      if (value != null) {
        values.put(rule.part(), value.value());
      } else if (rule.required()) {
        missing.add(rule.part());
      }
      parts.add(
          new PartSuggestion(
              rule.part(),
              rule.required(),
              rule.editable(),
              value == null ? null : value.value(),
              value == null ? null : value.origin(),
              o.base(),
              o.alternatives()));
    }
    String text = LabelTextComposer.compose(scheme.getLayout(), values, number);
    return new LabelSuggestion(
        scheme.getId(), parts, number, text, isTaken(libraryId, text), missing);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public LabelResponse createFor(Copy copy, Map<LabelPart, String> requested, Long actorId) {
    return persist(copy, requested, actorId);
  }

  @Transactional
  public LabelResponse createForExistingCopy(
      Long libraryId, Long copyId, Map<LabelPart, String> requested, Long actorId) {
    Copy copy =
        copyRepository
            .findDetailedById(copyId)
            .filter(c -> c.getLibrary().getId().equals(libraryId))
            .orElseThrow(() -> new ResourceNotFoundException("Copy not found in this library"));
    if (!copy.getStatus().holdsNumber()) {
      throw new BadRequestException(
          "A label cannot be created for a discarded or transferred copy");
    }
    return persist(copy, requested, actorId);
  }

  private LabelResponse persist(Copy copy, Map<LabelPart, String> requested, Long actorId) {
    if (labelRepository.existsByCopyId(copy.getId())) {
      throw new ConflictException("This copy already has a label");
    }
    Long libraryId = copy.getLibrary().getId();
    Long bookId = copy.getBook().getId();
    LabelScheme scheme = copy.getLibrary().getLabelScheme();
    Map<LabelPart, LabelValue> parts =
        resolve(scheme.getLayout(), bookId, requested, keptManualValues(libraryId, bookId));
    Label label =
        Label.builder()
            .library(copy.getLibrary())
            .book(copy.getBook())
            .copy(copy)
            .copyNumber(copy.getIteration())
            .build();
    apply(label, scheme, parts);

    saveAndFlush(label);
    writeRevision(label, actorId, INITIAL_REASON);
    return LabelResponse.fromEntity(label);
  }

  @Transactional
  public LabelResponse customize(
      Long libraryId, Long copyId, LabelCustomizeRequest request, Long actorId) {
    Label label = findActiveByCopy(libraryId, copyId);
    LabelScheme scheme = label.getLibrary().getLabelScheme();
    Map<LabelPart, LabelValue> parts =
        resolve(scheme.getLayout(), label.getBook().getId(), request.parts(), Map.of());
    if (parts.equals(label.getParts()) && scheme.equals(label.getScheme())) {
      throw new BadRequestException("The new label is the same as the current one");
    }
    apply(label, scheme, parts);
    label.setRevision(label.getRevision() + 1);
    resetPrint(label);

    saveAndFlush(label);
    writeRevision(label, actorId, request.changeReason());
    return LabelResponse.fromEntity(label);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void retire(Long copyId) {
    labelRepository
        .findByCopyId(copyId)
        .filter(label -> label.getRetiredAt() == null)
        .ifPresent(
            label -> {
              label.setRetiredAt(Instant.now());
              labelRepository.saveAndFlush(label);
            });
  }

  // A recovered book comes back with its label stuck on, so the same label returns to use
  @Transactional(propagation = Propagation.MANDATORY)
  public void reactivate(Copy copy, Long actorId) {
    labelRepository
        .findByCopyId(copy.getId())
        .filter(label -> label.getRetiredAt() != null)
        .ifPresent(
            label -> {
              int number = copy.getIteration();
              String text =
                  LabelTextComposer.compose(
                      label.getScheme().getLayout(), valuesOf(label.getParts()), number);
              boolean textTaken = isTaken(copy.getLibrary().getId(), text);

              label.setRetiredAt(null);
              if (number != label.getCopyNumber()) {
                label.setCopyNumber(number);
                label.setText(text);
                resetPrint(label);
              }
              if (textTaken) {
                resetPrint(label);
                label.setPrintStatus(PrintStatus.OUTDATED);
              }
              label.setRevision(label.getRevision() + 1);

              saveAndFlush(label);
              writeRevision(label, actorId, RECOVERED_REASON);
            });
  }

  @Transactional
  public List<LabelResponse> markPrinted(Long libraryId, Set<Long> labelIds, Long actorId) {
    List<Label> labels = labelRepository.findByIdIn(labelIds);
    if (labels.size() != labelIds.size()
        || labels.stream().anyMatch(l -> !l.getLibrary().getId().equals(libraryId))) {
      throw new ResourceNotFoundException("Some labels were not found");
    }
    if (labels.stream().anyMatch(l -> l.getRetiredAt() != null)) {
      throw new BadRequestException("Retired labels cannot be printed");
    }
    if (labels.stream().anyMatch(l -> l.getPrintStatus() == PrintStatus.OUTDATED)) {
      throw new BadRequestException("Outdated labels need a new text before printing");
    }
    Account actor = actorRef(actorId);
    Instant now = Instant.now();
    for (Label label : labels) {
      if (label.getPrintStatus() == PrintStatus.PRINTED) continue;
      label.setPrintStatus(PrintStatus.PRINTED);
      label.setPrintedAt(now);
      label.setPrintedBy(actor);
    }
    labelRepository.saveAll(labels);
    return labels.stream()
        .sorted(Comparator.comparing(Label::getId))
        .map(LabelResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<LabelResponse> findPendingPrint(Long libraryId) {
    return labelRepository
        .findActiveByLibraryAndPrintStatus(libraryId, PrintStatus.PENDING_PRINT)
        .stream()
        .map(LabelResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public LabelResponse getByCopyId(Long libraryId, Long copyId) {
    return LabelResponse.fromEntity(findByCopy(libraryId, copyId));
  }

  @Transactional(readOnly = true)
  public List<LabelRevisionResponse> getHistory(Long libraryId, Long copyId) {
    Label label = findByCopy(libraryId, copyId);
    return revisionRepository.findByLabelIdOrderByRevisionNumberDesc(label.getId()).stream()
        .map(LabelRevisionResponse::fromEntity)
        .toList();
  }

  // Helpers

  /**
   * Applies the scheme's rules to the values sent. A value that is the base or an alternative is
   * DERIVED; any other is MANUAL, allowed only on editable parts and to librarians. Parts not sent
   * take the kept manual value, else the base.
   */
  private Map<LabelPart, LabelValue> resolve(
      LabelLayout layout,
      Long bookId,
      Map<LabelPart, String> requested,
      Map<LabelPart, LabelValue> kept) {
    Map<LabelPart, String> sent = clean(requested);
    List<PartRule> rules = writableRules(layout);
    Set<LabelPart> writable = rules.stream().map(PartRule::part).collect(Collectors.toSet());
    for (LabelPart part : sent.keySet()) {
      if (!writable.contains(part)) {
        throw new BadRequestException("The library's label scheme has no writable part " + part);
      }
    }
    Map<LabelPart, Options> options = deriver.derive(catalogApi.getBookLabelData(bookId));
    boolean librarian = accessGuard.hasRole("LIBRARIAN");

    Map<LabelPart, LabelValue> parts = new EnumMap<>(LabelPart.class);
    List<LabelPart> missing = new ArrayList<>();
    boolean missingNeedsLibrarian = false;
    for (PartRule rule : rules) {
      Options o = options.get(rule.part());
      String value = sent.get(rule.part());
      LabelValue resolved;
      if (value == null) {
        resolved = defaultValue(o, kept.get(rule.part()));
      } else if (o.derives(value)) {
        resolved = new LabelValue(o.canonical(value), LabelOrigin.DERIVED);
      } else if (!rule.editable()) {
        throw new BadRequestException(
            rule.part() + " is not editable; use its derived value or an alternative");
      } else if (!librarian) {
        throw new ForbiddenException(
            ErrorCodes.REQUIRES_LIBRARIAN,
            "Only a librarian can write its own value for " + rule.part());
      } else {
        resolved = new LabelValue(value, LabelOrigin.MANUAL);
      }

      if (resolved != null) {
        parts.put(rule.part(), resolved);
      } else if (rule.required()) {
        missing.add(rule.part());
        missingNeedsLibrarian |= rule.editable() && !librarian;
      }
    }
    if (missingNeedsLibrarian) {
      throw new ForbiddenException(
          ErrorCodes.REQUIRES_LIBRARIAN,
          "Required parts without a derived value need a librarian: " + missing);
    }
    if (!missing.isEmpty()) {
      throw new BadRequestException("Required label parts have no value: " + missing);
    }
    return parts;
  }

  // The copy number belongs to the copy, so it is never sent or stored as a part
  private static List<PartRule> writableRules(LabelLayout layout) {
    return layout.rules().filter(r -> r.part() != LabelPart.COPY_NUMBER).toList();
  }

  private static LabelValue defaultValue(Options options, LabelValue kept) {
    if (kept != null) {
      return kept;
    }
    return options.base() == null ? null : new LabelValue(options.base(), LabelOrigin.DERIVED);
  }

  // A new copy of a book keeps the hand-written values of its labels, so only the number changes
  private Map<LabelPart, LabelValue> keptManualValues(Long libraryId, Long bookId) {
    return labelRepository
        .findFirstByLibraryIdAndBookIdAndRetiredAtIsNullOrderByIdDesc(libraryId, bookId)
        .map(
            label ->
                label.getParts().entrySet().stream()
                    .filter(e -> e.getValue().origin() == LabelOrigin.MANUAL)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
        .orElse(Map.of());
  }

  private static Map<LabelPart, String> clean(Map<LabelPart, String> requested) {
    Map<LabelPart, String> sent = new EnumMap<>(LabelPart.class);
    requested.forEach(
        (part, value) -> {
          if (part != null && value != null && !value.isBlank()) {
            sent.put(part, value.strip());
          }
        });
    return sent;
  }

  private static void apply(Label label, LabelScheme scheme, Map<LabelPart, LabelValue> parts) {
    label.setScheme(scheme);
    label.setParts(parts);
    label.setText(
        LabelTextComposer.compose(scheme.getLayout(), valuesOf(parts), label.getCopyNumber()));
  }

  private static Map<LabelPart, String> valuesOf(Map<LabelPart, LabelValue> parts) {
    Map<LabelPart, String> values = new EnumMap<>(LabelPart.class);
    parts.forEach((part, value) -> values.put(part, value.value()));
    return values;
  }

  private boolean isTaken(Long libraryId, String text) {
    return labelRepository.existsByLibraryIdAndTextIgnoreCaseAndRetiredAtIsNullAndPrintStatusNot(
        libraryId, text, PrintStatus.OUTDATED);
  }

  private Label findByCopy(Long libraryId, Long copyId) {
    return labelRepository
        .findByCopyId(copyId)
        .filter(l -> l.getLibrary().getId().equals(libraryId))
        .orElseThrow(() -> new ResourceNotFoundException("Label not found for this copy"));
  }

  private Label findActiveByCopy(Long libraryId, Long copyId) {
    Label label = findByCopy(libraryId, copyId);
    if (label.getRetiredAt() != null) {
      throw new BadRequestException("This label is retired and cannot be modified");
    }
    return label;
  }

  private static void resetPrint(Label label) {
    label.setPrintStatus(PrintStatus.PENDING_PRINT);
    label.setPrintedAt(null);
    label.setPrintedBy(null);
  }

  private void writeRevision(Label label, Long actorId, String reason) {
    revisionRepository.save(
        LabelRevision.builder()
            .label(label)
            .revisionNumber(label.getRevision())
            .scheme(label.getScheme())
            .parts(Map.copyOf(label.getParts()))
            .copyNumber(label.getCopyNumber())
            .text(label.getText())
            .changedBy(actorRef(actorId))
            .changeReason(reason)
            .build());
  }

  private Account actorRef(Long accountId) {
    return entityManager.getReference(Account.class, accountId);
  }

  private void saveAndFlush(Label label) {
    try {
      labelRepository.saveAndFlush(label);
    } catch (DataIntegrityViolationException e) {
      throw translate(e);
    }
  }

  private RuntimeException translate(DataIntegrityViolationException e) {
    String cause = String.valueOf(NestedExceptionUtils.getMostSpecificCause(e).getMessage());
    if (cause.contains("uq_label_library_text_lower")) {
      return new ConflictException("Another active label in this library already has this text");
    }
    if (cause.contains("uq_label_library_book_copy_number")) {
      return new ConflictException("This copy number is already in use for this book");
    }
    if (cause.contains("uq_label_copy")) {
      return new ConflictException("This copy already has a label");
    }
    return e;
  }
}
