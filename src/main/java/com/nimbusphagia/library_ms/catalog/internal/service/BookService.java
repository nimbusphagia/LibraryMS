package com.nimbusphagia.library_ms.catalog.internal.service;

import com.nimbusphagia.library_ms.catalog.internal.dto.book.BookFilter;
import com.nimbusphagia.library_ms.catalog.internal.dto.book.BookRequest;
import com.nimbusphagia.library_ms.catalog.internal.dto.book.BookResponse;
import com.nimbusphagia.library_ms.catalog.internal.dto.book.BookSummaryResponse;
import com.nimbusphagia.library_ms.catalog.internal.entity.Author;
import com.nimbusphagia.library_ms.catalog.internal.entity.Book;
import com.nimbusphagia.library_ms.catalog.internal.entity.Collection;
import com.nimbusphagia.library_ms.catalog.internal.entity.DeweyTag;
import com.nimbusphagia.library_ms.catalog.internal.entity.Publisher;
import com.nimbusphagia.library_ms.catalog.internal.repository.AuthorRepository;
import com.nimbusphagia.library_ms.catalog.internal.repository.BookRepository;
import com.nimbusphagia.library_ms.catalog.internal.repository.BookSpecifications;
import com.nimbusphagia.library_ms.catalog.internal.repository.CollectionRepository;
import com.nimbusphagia.library_ms.catalog.internal.repository.DeweyTagRepository;
import com.nimbusphagia.library_ms.catalog.internal.repository.PublisherRepository;
import com.nimbusphagia.library_ms.catalog.internal.repository.TagRepository;
import com.nimbusphagia.library_ms.exception.custom.BadRequestException;
import com.nimbusphagia.library_ms.exception.custom.ConflictException;
import com.nimbusphagia.library_ms.exception.custom.ResourceNotFoundException;
import java.util.HashSet;
import java.util.List;
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
  private final DeweyTagRepository deweyTagRepository;
  private final TagRepository tagRepository;

  @Transactional
  public BookResponse create(BookRequest request) {
    if (request.isbn() != null && bookRepository.existsByIsbn(request.isbn())) {
      throw new ConflictException("ISBN already exists");
    }
    Book book = new Book();
    apply(book, request);
    return BookResponse.fromEntity(bookRepository.save(book));
  }

  @Transactional
  public BookResponse edit(Long id, BookRequest request) {
    Book book =
        bookRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    if (request.isbn() != null && bookRepository.existsByIsbnAndIdNot(request.isbn(), id)) {
      throw new ConflictException("ISBN already exists");
    }
    apply(book, request);
    return BookResponse.fromEntity(book);
  }

  @Transactional(readOnly = true)
  public BookResponse findOne(Long id) {
    return bookRepository
        .findDetailById(id)
        .map(BookResponse::fromEntity)
        .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
  }

  @Transactional(readOnly = true)
  public Page<BookSummaryResponse> findAll(BookFilter filter, Pageable pageable) {
    return bookRepository
        .findAll(BookSpecifications.from(filter), pageable)
        .map(BookSummaryResponse::fromEntity);
  }

  @Transactional
  public BookResponse deactivate(Long id) {
    return setActive(id, false);
  }

  @Transactional
  public BookResponse activate(Long id) {
    return setActive(id, true);
  }

  // Helper
  private void apply(Book book, BookRequest r) {
    Publisher publisher = r.publisherId() == null ? null : findPublisher(r.publisherId());
    Collection collection = resolveCollection(r.collectionId(), r.publisherId());

    book.setTitle(r.title());
    book.setDescription(r.description());
    book.setAuthor(r.authorId() == null ? null : findAuthor(r.authorId()));
    book.setIllustrator(r.illustratorId() == null ? null : findAuthor(r.illustratorId()));
    book.setCollaborators(loadAll(r.collaboratorIds(), authorRepository, "Collaborator"));
    book.setPublisher(publisher);
    book.setCollection(collection);
    book.setCountry(r.country());
    book.setPublicationYear(r.publicationYear());
    book.setPageCount(r.pageCount());
    book.setIsbn(r.isbn());
    book.setDewey(r.deweyId() == null ? null : findDewey(r.deweyId()));
    book.setTags(loadAll(r.tagIds(), tagRepository, "Tag"));
  }

  private Collection resolveCollection(Long collectionId, Long publisherId) {
    if (collectionId == null) {
      return null;
    }
    if (publisherId == null) {
      throw new BadRequestException("A collection requires a publisher");
    }
    return collectionRepository
        .findByIdAndPublisherId(collectionId, publisherId)
        .orElseThrow(() -> new BadRequestException("Collection does not belong to this publisher"));
  }

  private Author findAuthor(Long id) {
    return authorRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + id));
  }

  private Publisher findPublisher(Long id) {
    return publisherRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + id));
  }

  private DeweyTag findDewey(Long id) {
    return deweyTagRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Dewey tag not found: " + id));
  }

  private <T> Set<T> loadAll(Set<Long> ids, JpaRepository<T, Long> repository, String label) {
    if (ids.isEmpty()) {
      return new HashSet<>();
    }
    List<T> found = repository.findAllById(ids);
    if (found.size() != ids.size()) {
      throw new ResourceNotFoundException(label + " not found");
    }
    return new HashSet<>(found);
  }

  private BookResponse setActive(Long id, boolean active) {
    Book book =
        bookRepository
            .findDetailById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    book.setActive(active);
    return BookResponse.fromEntity(book);
  }
}
