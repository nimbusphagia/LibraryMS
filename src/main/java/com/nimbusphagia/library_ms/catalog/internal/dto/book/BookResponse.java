package com.nimbusphagia.library_ms.catalog.internal.dto.book;

import com.nimbusphagia.library_ms.catalog.internal.dto.author.AuthorResponse;
import com.nimbusphagia.library_ms.catalog.internal.dto.collection.CollectionResponse;
import com.nimbusphagia.library_ms.catalog.internal.dto.deweyTag.DeweyTagResponse;
import com.nimbusphagia.library_ms.catalog.internal.dto.publisher.PublisherResponse;
import com.nimbusphagia.library_ms.catalog.internal.dto.tag.TagResponse;
import com.nimbusphagia.library_ms.catalog.internal.entity.Book;
import java.util.List;

public record BookResponse(
    Long id,
    String title,
    String description,
    AuthorResponse author,
    AuthorResponse illustrator,
    List<AuthorResponse> collaborators,
    PublisherResponse publisher,
    CollectionResponse collection,
    String country,
    Integer publicationYear,
    Integer pageCount,
    String isbn,
    DeweyTagResponse dewey,
    List<TagResponse> tags,
    boolean active) {

  public static BookResponse fromEntity(Book book) {
    return new BookResponse(
        book.getId(),
        book.getTitle(),
        book.getDescription(),
        book.getAuthor() == null ? null : AuthorResponse.fromEntity(book.getAuthor()),
        book.getIllustrator() == null ? null : AuthorResponse.fromEntity(book.getIllustrator()),
        book.getCollaborators().stream().map(AuthorResponse::fromEntity).toList(),
        book.getPublisher() == null ? null : PublisherResponse.fromEntity(book.getPublisher()),
        book.getCollection() == null ? null : CollectionResponse.fromEntity(book.getCollection()),
        book.getCountry(),
        book.getPublicationYear(),
        book.getPageCount(),
        book.getIsbn(),
        book.getDewey() == null ? null : DeweyTagResponse.fromEntity(book.getDewey()),
        book.getTags().stream().map(TagResponse::fromEntity).toList(),
        book.isActive());
  }
}
