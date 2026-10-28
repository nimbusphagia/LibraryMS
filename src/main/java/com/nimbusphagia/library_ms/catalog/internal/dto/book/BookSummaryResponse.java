package com.nimbusphagia.library_ms.catalog.internal.dto.book;

import com.nimbusphagia.library_ms.catalog.internal.entity.Author;
import com.nimbusphagia.library_ms.catalog.internal.entity.Book;

public record BookSummaryResponse(
    Long id, String title, String authorName, String publisherName, String isbn, boolean active) {

  public static BookSummaryResponse fromEntity(Book book) {
    Author author = book.getAuthor();
    String authorName =
        author == null
            ? null
            : (author.getFirstName()
                    + " "
                    + (author.getLastName() == null ? "" : author.getLastName()))
                .strip();
    return new BookSummaryResponse(
        book.getId(),
        book.getTitle(),
        authorName,
        book.getPublisher() == null ? null : book.getPublisher().getName(),
        book.getIsbn(),
        book.isActive());
  }
}
