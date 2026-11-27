package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.catalog.CatalogApi;
import com.nimbusphagia.library.catalog.LabelDataChanged;
import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.catalog.MalagaCode;
import com.nimbusphagia.library.exception.ErrorCodes;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ForbiddenException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelCustomizeRequest;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelImpact;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelRevisionResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestion;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestion.MalagaOption;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestion.MalagaSource;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabelService {
  private static final String INITIAL_REASON = "Initial label";
  private static final String RECOVERED_REASON = "Recovered";
  private static final String AUTO_REASON = "Updated automatically";
  private static final String REFRESHED_REASON = "Refreshed";

  private enum Outcome {
    UNCHANGED,
    UPDATED,
    OUTDATED
  }

  // A label rebuilt with its library's current scheme and catalog data, before it is applied
  private record Recomposition(
      LabelScheme scheme,
      Map<LabelPart, LabelValue> parts,
      String text,
      List<LabelPart> missing,
      boolean taken) {
    boolean usable() {
      return missing.isEmpty() && !taken;
    }
  }

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
    BookLabelData book = catalogApi.getBookLabelData(bookId);
    Map<LabelPart, Options> options = deriver.derive(book);
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
              o.alternatives(),
              rule.part() == LabelPart.AUTHOR_MALAGA
                  ? malagaOptions(libraryId, book)
                  : List.of()));
    }
    String text = LabelTextComposer.compose(scheme.getLayout(), values, number);
    return new LabelSuggestion(
        scheme.getId(), parts, number, text, isTaken(libraryId, text, null), missing);
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
    Map<LabelPart, Options> options = options(bookId);
    Map<LabelPart, LabelValue> parts =
        resolve(scheme.getLayout(), options, requested, keptManualValues(libraryId, bookId));
    Label label =
        Label.builder()
            .library(copy.getLibrary())
            .book(copy.getBook())
            .copy(copy)
            .copyNumber(copy.getIteration())
            .build();
    requireFreeText(label, scheme, parts, options);
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
    Map<LabelPart, Options> options = options(label.getBook().getId());
    Map<LabelPart, LabelValue> parts =
        resolve(scheme.getLayout(), options, request.parts(), Map.of());
    if (parts.equals(label.getParts())
        && scheme.equals(label.getScheme())
        && label.getPrintStatus() != PrintStatus.OUTDATED) {
      throw new BadRequestException("The new label is the same as the current one");
    }
    requireFreeText(label, scheme, parts, options);
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
              // Computed while the label is still retired, so its text cannot clash on flush
              Recomposition recomposition =
                  recomposition(label, copy.getIteration(), options(label.getBook().getId()));
              label.setRetiredAt(null);
              label.setCopyNumber(copy.getIteration());
              recompose(label, recomposition, actorId, RECOVERED_REASON);
              saveAndFlush(label);
            });
  }

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void onLabelDataChanged(LabelDataChanged event) {
    recomposeAll(
        labelRepository.findByBookIdInAndRetiredAtIsNull(event.bookIds()), event.actorId());
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public LabelImpact recomposeScheme(Long schemeId, Long actorId) {
    return recomposeAll(
        labelRepository.findByLibraryLabelSchemeIdAndRetiredAtIsNull(schemeId), actorId);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public LabelImpact recomposeLibrary(Long libraryId, Long actorId) {
    return recomposeAll(labelRepository.findByLibraryIdAndRetiredAtIsNull(libraryId), actorId);
  }

  /**
   * Gives an OUTDATED label its recomposed text. Parts sent replace the recomposed values with the
   * same rules as on creation, for example to pick an alternative when the text collides.
   */
  @Transactional
  public LabelResponse refresh(
      Long libraryId, Long copyId, Map<LabelPart, String> requested, Long actorId) {
    Label label = findActiveByCopy(libraryId, copyId);
    if (label.getPrintStatus() != PrintStatus.OUTDATED) {
      throw new BadRequestException("Only outdated labels can be refreshed");
    }
    Map<LabelPart, Options> options = options(label.getBook().getId());
    Recomposition recomposition = recomposition(label, label.getCopyNumber(), options);
    LabelScheme scheme = recomposition.scheme();
    Map<LabelPart, LabelValue> parts =
        resolve(scheme.getLayout(), options, requested, recomposition.parts());
    requireFreeText(label, scheme, parts, options);
    apply(label, scheme, parts);
    label.setRevision(label.getRevision() + 1);
    resetPrint(label);

    saveAndFlush(label);
    writeRevision(label, actorId, REFRESHED_REASON);
    return LabelResponse.fromEntity(label);
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
      Map<LabelPart, Options> options,
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

  private LabelImpact recomposeAll(List<Label> labels, Long actorId) {
    Set<Long> bookIds = labels.stream().map(l -> l.getBook().getId()).collect(Collectors.toSet());
    Map<Long, Map<LabelPart, Options>> options = new HashMap<>();
    catalogApi
        .getBookLabelData(bookIds)
        .forEach((id, book) -> options.put(id, deriver.derive(book)));
    int updated = 0;
    int outdated = 0;
    for (Label label : labels) {
      Recomposition recomposition =
          recomposition(label, label.getCopyNumber(), options.get(label.getBook().getId()));
      switch (recompose(label, recomposition, actorId, AUTO_REASON)) {
        case UPDATED -> updated++;
        case OUTDATED -> outdated++;
        case UNCHANGED -> {}
      }
    }
    return new LabelImpact(updated, outdated);
  }

  /**
   * DERIVED values that are still the base or an alternative stay, other DERIVED values take the
   * new base, MANUAL values stay; parts the scheme dropped go and new ones take their base.
   */
  private Recomposition recomposition(
      Label label, int copyNumber, Map<LabelPart, Options> options) {
    LabelScheme scheme = label.getLibrary().getLabelScheme();
    Map<LabelPart, LabelValue> parts = new EnumMap<>(LabelPart.class);
    List<LabelPart> missing = new ArrayList<>();
    for (PartRule rule : writableRules(scheme.getLayout())) {
      Options o = options.get(rule.part());
      LabelValue current = label.getParts().get(rule.part());
      boolean keep =
          current != null
              && (current.origin() == LabelOrigin.MANUAL || o.derives(current.value()));
      LabelValue value = keep ? current : defaultValue(o, null);
      if (value != null) {
        parts.put(rule.part(), value);
      } else if (rule.required()) {
        missing.add(rule.part());
      }
    }
    String text = LabelTextComposer.compose(scheme.getLayout(), valuesOf(parts), copyNumber);
    boolean taken = isTaken(label.getLibrary().getId(), text, label.getId());
    return new Recomposition(scheme, parts, text, missing, taken);
  }

  // A pending label follows the change by itself; a printed one, or one whose new text cannot be
  // used, waits as OUTDATED for a refresh
  private Outcome recompose(Label label, Recomposition r, Long actorId, String reason) {
    if (r.usable() && r.text().equals(label.getText())) {
      return Outcome.UNCHANGED;
    }
    if (r.usable() && label.getPrintStatus() == PrintStatus.PENDING_PRINT) {
      apply(label, r.scheme(), r.parts());
      label.setRevision(label.getRevision() + 1);
      saveAndFlush(label);
      writeRevision(label, actorId, reason);
      return Outcome.UPDATED;
    }
    if (label.getPrintStatus() == PrintStatus.OUTDATED) {
      return Outcome.UNCHANGED;
    }
    label.setPrintStatus(PrintStatus.OUTDATED);
    saveAndFlush(label);
    return Outcome.OUTDATED;
  }

  // Checked before changing a managed label, whose taken text would otherwise fail on auto-flush
  private void requireFreeText(
      Label label,
      LabelScheme scheme,
      Map<LabelPart, LabelValue> parts,
      Map<LabelPart, Options> options) {
    String text =
        LabelTextComposer.compose(scheme.getLayout(), valuesOf(parts), label.getCopyNumber());
    if (!isTaken(label.getLibrary().getId(), text, label.getId())) {
      return;
    }
    throw collision(text, scheme, options);
  }

  private static ConflictException collision(
      String text, LabelScheme scheme, Map<LabelPart, Options> options) {
    Map<LabelPart, List<String>> alternatives = new EnumMap<>(LabelPart.class);
    for (PartRule rule : writableRules(scheme.getLayout())) {
      List<String> found = options.get(rule.part()).alternatives();
      if (!found.isEmpty()) {
        alternatives.put(rule.part(), found);
      }
    }
    return new ConflictException(
        ErrorCodes.CONFLICT,
        "Another active label in this library already has the text " + text,
        Map.of("alternatives", alternatives));
  }

  // The author's codes on this library's labels, then the table. A Dewey number with decimals
  // also gets the one-digit form of each table code (Malaga rule 5: C4 besides C45)
  private List<MalagaOption> malagaOptions(Long libraryId, BookLabelData book) {
    List<MalagaOption> found = new ArrayList<>();
    if (book.authorId() != null) {
      labelRepository
          .findMalagaCodesOfAuthor(libraryId, book.authorId(), book.deweyNumber())
          .stream()
          .distinct()
          .map(code -> new MalagaOption(code, MalagaSource.AUTHOR_LABELS, null, null))
          .forEach(found::add);
    }
    boolean decimals = book.deweyNumber() != null && book.deweyNumber().contains(".");
    for (MalagaCode code : catalogApi.findMalagaCodes(deriver.malagaName(book))) {
      found.add(new MalagaOption(code.code(), MalagaSource.TABLE, code.entry(), code.qualifier()));
      String shortCode = code.code().replaceFirst("^(\\D+\\d)\\d+$", "$1");
      if (decimals && !shortCode.equals(code.code())) {
        found.add(new MalagaOption(shortCode, MalagaSource.TABLE, code.entry(), code.qualifier()));
      }
    }
    return found.stream().distinct().toList();
  }

  private Map<LabelPart, Options> options(Long bookId) {
    return deriver.derive(catalogApi.getBookLabelData(bookId));
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

  private boolean isTaken(Long libraryId, String text, Long labelId) {
    return labelRepository.isTextTaken(libraryId, text, labelId);
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
