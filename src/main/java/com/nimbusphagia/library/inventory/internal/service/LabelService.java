package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.catalog.CatalogApi;
import com.nimbusphagia.library.catalog.LabelDataChanged;
import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.catalog.MalagaCode;
import com.nimbusphagia.library.catalog.internal.entity.Book;
import com.nimbusphagia.library.exception.ErrorCodes;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ForbiddenException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.internal.dto.label.BookLabelResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.BookLabelRevisionResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.CopyLabelResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelCustomizeRequest;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelImpact;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelState;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestion.MalagaOption;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestion.MalagaSource;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestion.PartSuggestion;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestion;
import com.nimbusphagia.library.inventory.internal.dto.label.MarkPlacedRequest;
import com.nimbusphagia.library.inventory.internal.entity.BookLabel;
import com.nimbusphagia.library.inventory.internal.entity.BookLabelRevision;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;
import com.nimbusphagia.library.inventory.internal.entity.LabelLayout.PartRule;
import com.nimbusphagia.library.inventory.internal.entity.LabelLayout;
import com.nimbusphagia.library.inventory.internal.entity.LabelOrigin;
import com.nimbusphagia.library.inventory.internal.entity.LabelScheme;
import com.nimbusphagia.library.inventory.internal.entity.LabelValue;
import com.nimbusphagia.library.inventory.internal.entity.Library;
import com.nimbusphagia.library.inventory.internal.repository.BookLabelRepository;
import com.nimbusphagia.library.inventory.internal.repository.BookLabelRevisionRepository;
import com.nimbusphagia.library.inventory.internal.repository.CopyRepository;
import com.nimbusphagia.library.inventory.internal.repository.LibraryRepository;
import com.nimbusphagia.library.inventory.internal.service.LabelDeriver.Options;
import com.nimbusphagia.library.inventory.internal.util.LabelTextComposer;
import com.nimbusphagia.library.organization.AccessGuard;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A book's pattern in a library (BookLabel) holds every decision: the scheme, the part values and
 * the code. A copy only stores the label stuck on it; what it should show is the pattern with its
 * number, so changing a pattern never touches copies and their state is always derived.
 */
@Service
@RequiredArgsConstructor
public class LabelService {
  private static final String INITIAL_REASON = "Initial label";
  private static final String AUTO_REASON = "Updated automatically";
  private static final String REFRESHED_REASON = "Refreshed";

  // A pattern rebuilt with its library's current scheme and catalog data, before it is applied
  private record Recomposition(
      LabelScheme scheme, Map<LabelPart, LabelValue> parts, List<LabelPart> missing) {}

  private record PatternKey(Long libraryId, Long bookId) {}

  private final BookLabelRepository bookLabelRepository;
  private final BookLabelRevisionRepository revisionRepository;
  private final CopyRepository copyRepository;
  private final LibraryRepository libraryRepository;
  private final CatalogApi catalogApi;
  private final LabelDeriver deriver;
  private final AccessGuard accessGuard;
  private final EntityManager entityManager;

  // Suggestion

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
    Library library = libraryRepository.findById(libraryId).orElseThrow();
    if (catalogApi.findUsableBookIds(library.getInstitutionId(), Set.of(bookId)).isEmpty()) {
      throw new ResourceNotFoundException("Book not found");
    }
    Optional<BookLabel> pattern = bookLabelRepository.findByLibraryIdAndBookId(libraryId, bookId);
    LabelScheme scheme = pattern.map(BookLabel::getScheme).orElse(library.getLabelScheme());
    LabelLayout layout = pattern.map(BookLabel::getLayout).orElse(scheme.getLayout());
    BookLabelData book = catalogApi.getBookLabelData(bookId);
    Map<LabelPart, Options> options = deriver.derive(book);

    List<PartSuggestion> parts = new ArrayList<>();
    List<LabelPart> missing = new ArrayList<>();
    Map<LabelPart, LabelValue> chosen = new EnumMap<>(LabelPart.class);
    for (PartRule rule : writableRules(layout)) {
      Options o = options.get(rule.part());
      LabelValue value =
          pattern.isPresent() ? pattern.get().getParts().get(rule.part()) : baseOf(o);
      if (value != null) {
        chosen.put(rule.part(), value);
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
    String text = compose(layout, chosen, number);
    boolean collides =
        pattern.isEmpty() && clashes(libraryId, bookId, layout, chosen, List.of(number));
    return new LabelSuggestion(
        scheme.getId(), pattern.isPresent(), parts, number, text, collides, missing);
  }

  // Patterns

  @Transactional
  public BookLabelResponse create(
      Long libraryId, Long bookId, Map<LabelPart, String> requested, Long actorId) {
    Library library = libraryRepository.findById(libraryId).orElseThrow();
    if (catalogApi.findUsableBookIds(library.getInstitutionId(), Set.of(bookId)).isEmpty()) {
      throw new ResourceNotFoundException("Book not found");
    }
    if (bookLabelRepository.findByLibraryIdAndBookId(libraryId, bookId).isPresent()) {
      throw new ConflictException(
          "The book already has a label in this library; a librarian can customize it");
    }
    List<Integer> numbers =
        copyRepository
            .findByLibraryIdAndBookIdAndStatusIn(libraryId, bookId, CopyStatus.numberHolding())
            .stream()
            .map(Copy::getIteration)
            .toList();
    Book book = entityManager.getReference(Book.class, bookId);
    return BookLabelResponse.fromEntity(
        createPattern(library, book, requested, numbers, actorId));
  }

  // For copies registered with label parts: the first ones create the pattern, later ones follow
  @Transactional(propagation = Propagation.MANDATORY)
  public BookLabel ensurePattern(
      Library library,
      Book book,
      Map<LabelPart, String> requested,
      List<Integer> numbers,
      Long actorId) {
    return bookLabelRepository
        .findByLibraryIdAndBookId(library.getId(), book.getId())
        .map(existing -> requireSameParts(existing, requested))
        .orElseGet(() -> createPattern(library, book, requested, numbers, actorId));
  }

  private BookLabel requireSameParts(BookLabel pattern, Map<LabelPart, String> requested) {
    clean(requested)
        .forEach(
            (part, value) -> {
              LabelValue current = pattern.getParts().get(part);
              if (current == null || !current.value().equalsIgnoreCase(value)) {
                throw new BadRequestException(
                    "Every copy of the book follows its label in this library; a librarian can"
                        + " change it for all of them");
              }
            });
    return pattern;
  }

  private BookLabel createPattern(
      Library library,
      Book book,
      Map<LabelPart, String> requested,
      List<Integer> numbers,
      Long actorId) {
    LabelScheme scheme = library.getLabelScheme();
    Map<LabelPart, Options> options = options(book.getId());
    Map<LabelPart, LabelValue> parts =
        resolve(scheme.getLayout(), options, requested, Map.of());
    BookLabel pattern = BookLabel.builder().library(library).book(book).build();
    requireFree(pattern, scheme, parts, options, numbers);
    setParts(pattern, scheme, parts);
    saveAndFlush(pattern);
    writeRevision(pattern, actorId, INITIAL_REASON);
    return pattern;
  }

  @Transactional(readOnly = true)
  public BookLabelResponse getPattern(Long libraryId, Long bookId) {
    return BookLabelResponse.fromEntity(findPattern(libraryId, bookId));
  }

  @Transactional(readOnly = true)
  public List<BookLabelRevisionResponse> getHistory(Long libraryId, Long bookId) {
    return revisionRepository
        .findByBookLabelIdOrderByRevisionNumberDesc(findPattern(libraryId, bookId).getId())
        .stream()
        .map(BookLabelRevisionResponse::fromEntity)
        .toList();
  }

  // A librarian's change of the book's label; copies with the old one stuck become outdated
  @Transactional
  public BookLabelResponse customize(
      Long libraryId, Long bookId, LabelCustomizeRequest request, Long actorId) {
    BookLabel pattern = findPattern(libraryId, bookId);
    LabelScheme scheme = pattern.getLibrary().getLabelScheme();
    Map<LabelPart, Options> options = options(bookId);
    Map<LabelPart, LabelValue> parts =
        resolve(scheme.getLayout(), options, request.parts(), Map.of());
    if (parts.equals(pattern.getParts())
        && scheme.equals(pattern.getScheme())
        && scheme.getLayout().equals(pattern.getLayout())
        && !pattern.isOutdated()) {
      throw new BadRequestException("The new label is the same as the current one");
    }
    requireFree(pattern, scheme, parts, options, numbers(pattern));
    change(pattern, scheme, parts, actorId, request.changeReason());
    return BookLabelResponse.fromEntity(pattern);
  }

  /**
   * Settles an outdated pattern: it takes its recomposition, with the parts sent replacing values
   * under the creation rules (for example, an alternative when the code collides).
   */
  @Transactional
  public BookLabelResponse refresh(
      Long libraryId, Long bookId, Map<LabelPart, String> requested, Long actorId) {
    BookLabel pattern = findPattern(libraryId, bookId);
    if (!pattern.isOutdated()) {
      throw new BadRequestException(
          "The book's label is up to date; a librarian can customize it");
    }
    Map<LabelPart, Options> options = options(bookId);
    Recomposition r = recomposition(pattern, options);
    Map<LabelPart, LabelValue> parts =
        resolve(r.scheme().getLayout(), options, requested, r.parts());
    requireFree(pattern, r.scheme(), parts, options, numbers(pattern));
    change(pattern, r.scheme(), parts, actorId, REFRESHED_REASON);
    return BookLabelResponse.fromEntity(pattern);
  }

  // Recomposition

  @EventListener
  @Transactional(propagation = Propagation.MANDATORY)
  public void onLabelDataChanged(LabelDataChanged event) {
    recomposeAll(bookLabelRepository.findByBookIdIn(event.bookIds()), event.actorId());
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public LabelImpact recomposeScheme(Long schemeId, Long actorId) {
    return recomposeAll(bookLabelRepository.findByLibraryLabelSchemeId(schemeId), actorId);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public LabelImpact recomposeLibrary(Long libraryId, Long actorId) {
    return recomposeAll(bookLabelRepository.findByLibraryId(libraryId), actorId);
  }

  private LabelImpact recomposeAll(List<BookLabel> patterns, Long actorId) {
    Set<Long> bookIds =
        patterns.stream().map(p -> p.getBook().getId()).collect(Collectors.toSet());
    Map<Long, Map<LabelPart, Options>> options = new HashMap<>();
    catalogApi
        .getBookLabelData(bookIds)
        .forEach((id, book) -> options.put(id, deriver.derive(book)));
    LabelImpact impact = LabelImpact.NONE;
    for (BookLabel pattern : patterns) {
      impact = impact.plus(recompose(pattern, options.get(pattern.getBook().getId()), actorId));
    }
    return impact;
  }

  // A pattern that cannot take its recomposition keeps its parts, so its copies stay coherent,
  // and is marked outdated until a refresh or a librarian settles it
  private LabelImpact recompose(
      BookLabel pattern, Map<LabelPart, Options> options, Long actorId) {
    Recomposition r = recomposition(pattern, options);
    if (r.parts().equals(pattern.getParts())
        && r.scheme().equals(pattern.getScheme())
        && r.scheme().getLayout().equals(pattern.getLayout())) {
      if (!pattern.isOutdated()) {
        return LabelImpact.NONE;
      }
      // The data that outdated it came back
      pattern.setOutdated(false);
      saveAndFlush(pattern);
      return LabelImpact.NONE;
    }
    Long libraryId = pattern.getLibrary().getId();
    Long bookId = pattern.getBook().getId();
    if (!r.missing().isEmpty()
        || clashes(libraryId, bookId, r.scheme().getLayout(), r.parts(), numbers(pattern))) {
      if (pattern.isOutdated()) {
        return LabelImpact.NONE;
      }
      pattern.setOutdated(true);
      saveAndFlush(pattern);
      return new LabelImpact(0, 0, 1);
    }
    return change(pattern, r.scheme(), r.parts(), actorId, AUTO_REASON);
  }

  /**
   * DERIVED values that are still the base or an alternative stay, other DERIVED values take the
   * new base, MANUAL values stay; parts the scheme dropped go and new ones take their base.
   */
  private Recomposition recomposition(BookLabel pattern, Map<LabelPart, Options> options) {
    LabelScheme scheme = pattern.getLibrary().getLabelScheme();
    Map<LabelPart, LabelValue> parts = new EnumMap<>(LabelPart.class);
    List<LabelPart> missing = new ArrayList<>();
    for (PartRule rule : writableRules(scheme.getLayout())) {
      Options o = options.get(rule.part());
      LabelValue current = pattern.getParts().get(rule.part());
      boolean keep =
          current != null
              && (current.origin() == LabelOrigin.MANUAL || o.derives(current.value()));
      LabelValue value = keep ? current : baseOf(o);
      if (value != null) {
        parts.put(rule.part(), value);
      } else if (rule.required()) {
        missing.add(rule.part());
      }
    }
    return new Recomposition(scheme, parts, missing);
  }

  // The pattern takes the parts, already checked to be free
  private LabelImpact change(
      BookLabel pattern,
      LabelScheme scheme,
      Map<LabelPart, LabelValue> parts,
      Long actorId,
      String reason) {
    List<Copy> labelled =
        activeCopies(pattern).stream()
            .filter(c -> pattern.textFor(c.getIteration()).equals(c.getPlacedText()))
            .toList();
    setParts(pattern, scheme, parts);
    pattern.setOutdated(false);
    pattern.setRevision(pattern.getRevision() + 1);
    saveAndFlush(pattern);
    writeRevision(pattern, actorId, reason);
    int outdated =
        (int)
            labelled.stream()
                .filter(c -> !pattern.textFor(c.getIteration()).equals(c.getPlacedText()))
                .count();
    return new LabelImpact(1, outdated, 0);
  }

  // Copies

  // Transaction only
  public Map<Long, CopyLabelResponse> labelsOf(Collection<Copy> copies) {
    if (copies.isEmpty()) return Map.of();
    Set<Long> bookIds = copies.stream().map(c -> c.getBook().getId()).collect(Collectors.toSet());
    Map<PatternKey, BookLabel> patterns =
        bookLabelRepository.findByBookIdIn(bookIds).stream()
            .collect(
                Collectors.toMap(
                    p -> new PatternKey(p.getLibrary().getId(), p.getBook().getId()), p -> p));
    Map<Long, CopyLabelResponse> labels = new HashMap<>();
    for (Copy c : copies) {
      BookLabel pattern =
          patterns.get(new PatternKey(c.getLibrary().getId(), c.getBook().getId()));
      labels.put(c.getId(), CopyLabelResponse.of(c, pattern));
    }
    return labels;
  }

  @Transactional(readOnly = true)
  public CopyLabelResponse getByCopyId(Long libraryId, Long copyId) {
    Copy copy =
        copyRepository
            .findDetailedById(copyId)
            .filter(c -> c.getLibrary().getId().equals(libraryId))
            .orElseThrow(() -> new ResourceNotFoundException("Copy not found in this library"));
    return labelsOf(List.of(copy)).get(copyId);
  }

  // Copies on the shelves to label or relabel, leaving out books whose pattern waits a refresh,
  // since their text is about to change
  @Transactional(readOnly = true)
  public List<CopyLabelResponse> findPending(Long libraryId) {
    Map<Long, BookLabel> patterns =
        bookLabelRepository.findByLibraryId(libraryId).stream()
            .filter(p -> !p.isOutdated())
            .collect(Collectors.toMap(p -> p.getBook().getId(), p -> p));
    return copyRepository
        .findByLibraryIdAndStatusInOrderById(libraryId, CopyStatus.numberHolding())
        .stream()
        .filter(c -> patterns.containsKey(c.getBook().getId()))
        .map(c -> CopyLabelResponse.of(c, patterns.get(c.getBook().getId())))
        .filter(l -> l.state() == LabelState.UNLABELLED || l.state() == LabelState.OUTDATED)
        .toList();
  }

  @Transactional
  public List<CopyLabelResponse> markPlaced(
      Long libraryId, List<MarkPlacedRequest.Placed> placed, Long actorId) {
    Map<Long, String> texts = new HashMap<>();
    for (MarkPlacedRequest.Placed p : placed) {
      if (texts.put(p.copyId(), p.text().strip()) != null) {
        throw new BadRequestException("Copy " + p.copyId() + " appears more than once");
      }
    }
    List<Copy> copies = copyRepository.findAllByIdInForUpdate(texts.keySet());
    if (copies.size() != texts.size()
        || copies.stream().anyMatch(c -> !c.getLibrary().getId().equals(libraryId))) {
      throw new ResourceNotFoundException("Some copies were not found in this library");
    }
    if (copies.stream().anyMatch(c -> !c.getStatus().holdsNumber())) {
      throw new BadRequestException("Only copies on the shelves can be labelled");
    }
    Set<Long> bookIds = copies.stream().map(c -> c.getBook().getId()).collect(Collectors.toSet());
    Map<Long, BookLabel> patterns =
        bookLabelRepository.findByLibraryIdAndBookIdIn(libraryId, bookIds).stream()
            .collect(Collectors.toMap(p -> p.getBook().getId(), p -> p));

    List<Copy> changing = new ArrayList<>();
    for (Copy copy : copies) {
      BookLabel pattern = patterns.get(copy.getBook().getId());
      if (pattern == null) {
        throw new BadRequestException(
            "Copy " + copy.getId() + ": the book has no label in this library");
      }
      // Its text is about to change, so the label just placed would be wasted
      if (pattern.isOutdated()) {
        throw new BadRequestException(
            "Copy " + copy.getId() + ": the book's label needs a refresh first");
      }
      String expected = pattern.textFor(copy.getIteration());
      if (!expected.equals(texts.get(copy.getId()))) {
        throw new ConflictException(
            "Copy " + copy.getId() + ": its label changed to " + expected + "; print it again");
      }
      if (!expected.equals(copy.getPlacedText())) {
        changing.add(copy);
      }
    }

    // Cleared first, so copies swapping texts within the batch never meet the unique index
    changing.forEach(Copy::clearPlaced);
    saveAndFlush(changing);
    Account actor = actorRef(actorId);
    Instant now = Instant.now();
    changing.forEach(c -> c.place(texts.get(c.getId()), now, actor));
    saveAndFlush(changing);

    Map<Long, CopyLabelResponse> labels = labelsOf(copies);
    return copies.stream().map(c -> labels.get(c.getId())).toList();
  }

  // Helpers

  /**
   * Applies the scheme's rules to the values sent. A value that is the base or an alternative is
   * DERIVED; any other is MANUAL, allowed only on editable parts and to librarians. Parts not sent
   * take the kept value, else the base.
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
        resolved = kept.containsKey(rule.part()) ? kept.get(rule.part()) : baseOf(o);
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

  private static LabelValue baseOf(Options options) {
    return options.base() == null ? null : new LabelValue(options.base(), LabelOrigin.DERIVED);
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

  private static String compose(
      LabelLayout layout, Map<LabelPart, LabelValue> parts, Integer copyNumber) {
    return LabelTextComposer.compose(layout, BookLabel.values(parts), copyNumber);
  }

  private static void setParts(
      BookLabel pattern, LabelScheme scheme, Map<LabelPart, LabelValue> parts) {
    pattern.setScheme(scheme);
    pattern.setLayout(scheme.getLayout());
    pattern.setParts(parts);
    pattern.setCode(compose(scheme.getLayout(), parts, null));
  }

  /**
   * Whether the parts would give the book a code another book of the library has, or, for one of
   * these copy numbers, a text stuck on a copy of another book, even an outdated one. Checked
   * before a managed entity changes, since a taken value would otherwise fail on the next
   * auto-flush.
   */
  private boolean clashes(
      Long libraryId,
      Long bookId,
      LabelLayout layout,
      Map<LabelPart, LabelValue> parts,
      List<Integer> copyNumbers) {
    return bookLabelRepository.existsByLibraryIdAndCodeIgnoreCaseAndBookIdNot(
            libraryId, compose(layout, parts, null), bookId)
        || copyNumbers.stream()
            .anyMatch(
                n ->
                    copyRepository.isPlacedOnOtherBook(
                        libraryId, compose(layout, parts, n), bookId, CopyStatus.numberHolding()));
  }

  private void requireFree(
      BookLabel pattern,
      LabelScheme scheme,
      Map<LabelPart, LabelValue> parts,
      Map<LabelPart, Options> options,
      List<Integer> copyNumbers) {
    LabelLayout layout = scheme.getLayout();
    if (!clashes(
        pattern.getLibrary().getId(), pattern.getBook().getId(), layout, parts, copyNumbers)) {
      return;
    }
    Map<LabelPart, List<String>> alternatives = new EnumMap<>(LabelPart.class);
    for (PartRule rule : writableRules(layout)) {
      List<String> found = options.get(rule.part()).alternatives();
      if (!found.isEmpty()) {
        alternatives.put(rule.part(), found);
      }
    }
    throw new ConflictException(
        ErrorCodes.CONFLICT,
        "Another book of this library already has the label " + compose(layout, parts, null),
        Map.of("alternatives", alternatives));
  }

  private List<Integer> numbers(BookLabel pattern) {
    return activeCopies(pattern).stream().map(Copy::getIteration).toList();
  }

  private List<Copy> activeCopies(BookLabel pattern) {
    return copyRepository.findByLibraryIdAndBookIdAndStatusIn(
        pattern.getLibrary().getId(), pattern.getBook().getId(), CopyStatus.numberHolding());
  }

  // The author's codes in this library, then the table. A Dewey number with decimals also gets
  // the one-digit form of each table code (Malaga rule 5: C4 besides C45)
  private List<MalagaOption> malagaOptions(Long libraryId, BookLabelData book) {
    List<MalagaOption> found = new ArrayList<>();
    if (book.authorId() != null) {
      bookLabelRepository
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

  private BookLabel findPattern(Long libraryId, Long bookId) {
    return bookLabelRepository
        .findByLibraryIdAndBookId(libraryId, bookId)
        .orElseThrow(() -> new ResourceNotFoundException("The book has no label in this library"));
  }

  private void writeRevision(BookLabel pattern, Long actorId, String reason) {
    revisionRepository.save(
        BookLabelRevision.builder()
            .bookLabel(pattern)
            .revisionNumber(pattern.getRevision())
            .scheme(pattern.getScheme())
            .parts(Map.copyOf(pattern.getParts()))
            .code(pattern.getCode())
            .changedBy(actorRef(actorId))
            .changeReason(reason)
            .build());
  }

  private Account actorRef(Long accountId) {
    return entityManager.getReference(Account.class, accountId);
  }

  private void saveAndFlush(List<Copy> copies) {
    try {
      copyRepository.saveAllAndFlush(copies);
    } catch (DataIntegrityViolationException e) {
      throw translate(e);
    }
  }

  private void saveAndFlush(BookLabel pattern) {
    try {
      bookLabelRepository.saveAndFlush(pattern);
    } catch (DataIntegrityViolationException e) {
      throw translate(e);
    }
  }

  // Concurrent writers that passed the checks meet the indexes
  private RuntimeException translate(DataIntegrityViolationException e) {
    String cause = String.valueOf(NestedExceptionUtils.getMostSpecificCause(e).getMessage());
    if (cause.contains("uq_book_label_library_code_lower")) {
      return new ConflictException("Another book of this library already has this label");
    }
    if (cause.contains("uq_copy_library_placed_text_lower")) {
      return new ConflictException("Another copy of this library already shows this label");
    }
    if (cause.contains("uq_book_label_library_book")) {
      return new ConflictException("The book's label was created meanwhile; please retry");
    }
    return e;
  }
}
