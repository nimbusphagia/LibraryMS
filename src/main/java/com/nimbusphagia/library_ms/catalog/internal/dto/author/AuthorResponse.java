package com.nimbusphagia.library_ms.catalog.internal.dto.author;

import com.nimbusphagia.library_ms.catalog.internal.entity.Author;

public record AuthorResponse(Long id, String firstName, String lastName) {
  public static AuthorResponse fromEntity(Author author) {
    return new AuthorResponse(author.getId(), author.getFirstName(), author.getLastName());
  }
}
