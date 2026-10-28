package com.nimbusphagia.library_ms.catalog.internal.dto.author;

import com.nimbusphagia.library_ms.catalog.internal.entity.Author;
import jakarta.validation.constraints.NotBlank;

public record AuthorRequest(
    @NotBlank(message = "Author first name is missing") String firstName, String lastName) {

  public AuthorRequest {
    firstName = firstName == null ? null : firstName.strip();
    lastName = (lastName == null || lastName.isBlank()) ? null : lastName.strip();
  }

  public Author toEntity() {
    return Author.builder().firstName(firstName).lastName(lastName).build();
  }

  public Author replace(Author author) {
    author.setFirstName(firstName);
    author.setLastName(lastName);
    return author;
  }
}
