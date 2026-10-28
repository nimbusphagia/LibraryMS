package com.nimbusphagia.library_ms.catalog.internal.dto.author;

import com.nimbusphagia.library_ms.catalog.internal.entity.Author;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Valid
public record AuthorRequest(
    @NotBlank(message = "Author first name is missing") String firstName, String lastName) {
  public Author toEntity() {
    return Author.builder().firstName(firstName).lastName(lastName).build();
  }

  public Author replace(Author author) {
    author.setFirstName(firstName);
    author.setLastName(lastName);
    return author;
  }
}
