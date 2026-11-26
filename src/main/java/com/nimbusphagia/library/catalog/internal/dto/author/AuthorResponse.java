package com.nimbusphagia.library.catalog.internal.dto.author;

import com.nimbusphagia.library.catalog.internal.entity.Author;

public record AuthorResponse(Long id, Long institutionId, String firstName, String lastName) {
  public static AuthorResponse fromEntity(Author author) {
    return new AuthorResponse(
        author.getId(), author.getInstitutionId(), author.getFirstName(), author.getLastName());
  }
}
