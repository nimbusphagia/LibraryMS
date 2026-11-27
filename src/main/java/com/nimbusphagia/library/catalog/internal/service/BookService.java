package com.nimbusphagia.library.catalog.internal.service;

import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.idNot;

import com.nimbusphagia.library.catalog.LabelledBooks;
import com.nimbusphagia.library.catalog.internal.dto.book.BookFilter;
import com.nimbusphagia.library.catalog.internal.dto.book.BookRequest;
import com.nimbusphagia.library.catalog.internal.dto.book.BookResponse;
import com.nimbusphagia.library.catalog.internal.dto.book.BookSummaryResponse;
import com.nimbusphagia.library.catalog.internal.entity.Author;
import com.nimbusphagia.library.catalog.internal.entity.Book;
import com.nimbusphagia.library.catalog.internal.entity.CatalogEntity;
import com.nimbusphagia.library.catalog.internal.entity.Category;
import com.nimbusphagia.library.catalog.internal.entity.Collection;
import com.nimbusphagia.library.catalog.internal.entity.Publisher;
import com.nimbusphagia.library.catalog.internal.entity.Series;
import com.nimbusphagia.library.catalog.internal.entity.Subcategory;
import com.nimbusphagia.library.catalog.internal.repository.AuthorRepository;
import com.nimbusphagia.library.catalog.internal.repository.BookRepository;
import com.nimbusphagia.library.catalog.internal.repository.BookSpecifications;
import com.nimbusphagia.library.catalog.internal.repository.CategoryRepository;
import com.nimbusphagia.library.catalog.internal.repository.CollectionRepository;
import com.nimbusphagia.library.catalog.internal.repository.PublisherRepository;
import com.nimbusphagia.library.catalog.internal.repository.SeriesRepository;
import com.nimbusphagia.library.catalog.internal.repository.SubcategoryRepository;
import com.nimbusphagia.library.catalog.internal.repository.TagRepository;
import com.nimbusphagia.library.exception.ErrorCodes;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {
  private final BookRepository bookRepository;
  private final AuthorRepository authorRepository;
  private final PublisherRepository publisherRepository;
  private final CollectionRepository collectionRepository;
  private final TagRepository tagRepository;
  private final SeriesRepository seriesRepository;
  private final CategoryRepository categoryRepository;
  private final SubcategoryRepository subcategoryRepository;
  private final LabelledBooks labelledBooks;
  private final CatalogAccess access;

  @Transactional
  public BookResponse create(Long institutionId, BookRequest request) {
    Book book = access.stamp(new Book(), institutionId);
    requireUniqueIsbn(institutionId, request.isbn(), null);
    apply(book, request);
    return BookResponse.fromEntity(bookRepository.save(book));
  }

  @Transactional
  public BookResponse edit(Long institutionId, Long id, BookRequest request) {
    Book book = findOwned(institutionId, id);
    requireUniqueIsbn(institutionId, request.isbn(), id);
    // Labels copy this data when printed; only admins may leave them outdated
    if (changesLabelData(book, request)
        && !access.isAdmin()
        && labelledBooks.anyLabelled(Set.of(id))) {
      throw new ConflictException(
          ErrorCodes.LABEL_DATA_LOCKED,
          "The book has labels; ask an admin to change its title, category, subcategory, Dewey or"
              + " volumes");
    }
    apply(book, request);
    return BookResponse.fromEntity(book);
  }

  @Transactional(readOnly = true)
  public BookResponse findOne(Long institutionId, Long id) {
    return access
        .visible(bookRepository.findDetailById(id), institutionId)
        .map(BookResponse::fromEntity)
        .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
  }

  @Transactional(readOnly = true)
  public Page<BookSummaryResponse> findAll(
      Long institutionId, BookFilter filter, Pageable pageable) {
    return bookRepository
        .findAll(
            access.<Book>in(institutionId, filter.scope()).and(BookSpecifications.from(filter)),
            pageable)
        .map(BookSummaryResponse::fromEntity);
  }

  @Transactional
  public BookResponse deactivate(Long institutionId, Long id) {
    return setActive(institutionId, id, false);
  }

  @Transactional
  public BookResponse activate(Long institutionId, Long id) {
    return setActive(institutionId, id, true);
  }

  // Helper
  private Book findOwned(Long institutionId, Long id) {
    access.requireWriter(institutionId);
    return access
        .owned(bookRepository.findDetailById(id), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
  }

  private void requireUniqueIsbn(Long institutionId, String isbn, Long id) {
    if (isbn != null
        && bookRepository.exists(
            access
                .<Book>visible(institutionId)
                .and((root, query, cb) -> cb.equal(root.get("isbn"), isbn))
                .and(idNot(id)))) {
      throw new ConflictException("ISBN already exists");
    }
  }

  private static boolean changesLabelData(Book book, BookRequest r) {
    Long categoryId = book.getCategory() == null ? null : book.getCategory().getId();
    Long subcategoryId = book.getSubcategory() == null ? null : book.getSubcategory().getId();
    return !book.getTitle().equals(r.title())
        || !Objects.equals(categoryId, r.categoryId())
        || !Objects.equals(subcategoryId, r.subcategoryId())
        || !Objects.equals(book.getDeweyNumber(), r.deweyNumber())
        || !Objects.equals(book.getCollectionVolume(), r.collectionVolume())
        || !Objects.equals(book.getSeriesVolume(), r.seriesVolume());
  }

  private void apply(Book book, BookRequest r) {
    Long scope = book.getInstitutionId();
    Author author = r.authorId() == null ? null : findAuthor(scope, r.authorId());
    Publisher publisher = r.publisherId() == null ? null : findPublisher(scope, r.publisherId());
    Collection collection = resolveCollection(scope, r.collectionId(), r.publisherId());
    Series series = resolveSeries(scope, r.seriesId(), r.authorId());
    Subcategory subcategory = resolveSubcategory(scope, r.subcategoryId(), r.categoryId());

    if (r.collectionVolume() != null && collection == null) {
      throw new BadRequestException("A collection volume requires a collection");
    }
    if (r.seriesVolume() != null && series == null) {
      throw new BadRequestException("A series volume requires a series");
    }

    book.setTitle(r.title());
    book.setDescription(r.description());
    book.setAuthor(author);
    book.setIllustrator(r.illustratorId() == null ? null : findAuthor(scope, r.illustratorId()));
    book.setCollaborators(loadAll(scope, r.collaboratorIds(), authorRepository, "Collaborator"));
    book.setPublisher(publisher);
    book.setCollection(collection);
    book.setCollectionVolume(r.collectionVolume());
    book.setSeries(series);
    book.setSeriesVolume(r.seriesVolume());
    book.setCategory(r.categoryId() == null ? null : findCategory(scope, r.categoryId()));
    book.setSubcategory(subcategory);
    book.setCountry(r.country());
    book.setPublicationYear(r.publicationYear());
    book.setPageCount(r.pageCount());
    book.setIsbn(r.isbn());
    book.setDeweyNumber(r.deweyNumber());
    book.setTags(loadAll(scope, r.tagIds(), tagRepository, "Tag"));
  }

  private Collection resolveCollection(Long scope, Long collectionId, Long publisherId) {
    if (collectionId == null) {
      return null;
    }
    if (publisherId == null) {
      throw new BadRequestException("A collection requires a publisher");
    }
    return access
        .visible(collectionRepository.findByIdAndPublisherId(collectionId, publisherId), scope)
        .orElseThrow(() -> new BadRequestException("Collection does not belong to this publisher"));
  }

  private Series resolveSeries(Long scope, Long seriesId, Long authorId) {
    if (seriesId == null) {
      return null;
    }
    if (authorId == null) {
      throw new BadRequestException("A series requires an author");
    }
    return access
        .visible(seriesRepository.findByIdAndAuthorId(seriesId, authorId), scope)
        .orElseThrow(() -> new BadRequestException("Series does not belong to this author"));
  }

  private Subcategory resolveSubcategory(Long scope, Long subcategoryId, Long categoryId) {
    if (subcategoryId == null) {
      return null;
    }
    if (categoryId == null) {
      throw new BadRequestException("A subcategory requires a category");
    }
    return access
        .visible(subcategoryRepository.findByIdAndCategoryId(subcategoryId, categoryId), scope)
        .orElseThrow(
            () -> new BadRequestException("Subcategory does not belong to this category"));
  }

  private Category findCategory(Long scope, Long id) {
    return visibleOrNotFound(categoryRepository.findById(id), scope, "Category not found: " + id);
  }

  private Author findAuthor(Long scope, Long id) {
    return visibleOrNotFound(authorRepository.findById(id), scope, "Author not found: " + id);
  }

  private Publisher findPublisher(Long scope, Long id) {
    return visibleOrNotFound(publisherRepository.findById(id), scope, "Publisher not found: " + id);
  }

  private <T extends CatalogEntity> T visibleOrNotFound(
      Optional<T> entity, Long scope, String message) {
    return access.visible(entity, scope).orElseThrow(() -> new ResourceNotFoundException(message));
  }

  private <T extends CatalogEntity> Set<T> loadAll(
      Long scope, Set<Long> ids, JpaRepository<T, Long> repository, String label) {
    if (ids.isEmpty()) {
      return new HashSet<>();
    }
    List<T> found =
        repository.findAllById(ids).stream().filter(e -> access.isVisible(e, scope)).toList();
    if (found.size() != ids.size()) {
      throw new ResourceNotFoundException(label + " not found");
    }
    return new HashSet<>(found);
  }

  private BookResponse setActive(Long institutionId, Long id, boolean active) {
    Book book = findOwned(institutionId, id);
    book.setActive(active);
    return BookResponse.fromEntity(book);
  }
}
