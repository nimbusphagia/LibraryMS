package com.nimbusphagia.library_ms.catalog.internal.dto.book;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record BookRequest(
    @NotBlank(message = "Book title is missing") @Size(max = 255) String title,
    String description,
    Long authorId,
    Long illustratorId,
    Set<Long> collaboratorIds,
    Long publisherId,
    Long collectionId,
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be an ISO 3166 alpha-2 code")
        String country,
    @Min(1) @Max(2100) Integer publicationYear,
    @Positive Integer pageCount,
    @Pattern(regexp = "^(\\d{9}[\\dX]|\\d{13})$", message = "Invalid ISBN") String isbn,
    Long deweyId,
    Set<Long> tagIds) {

  public BookRequest {
    title = title == null ? null : title.strip();
    description = blankToNull(description);
    country = country == null || country.isBlank() ? null : country.strip().toUpperCase();
    isbn = isbn == null || isbn.isBlank() ? null : isbn.replaceAll("[\\s-]", "").toUpperCase();
    collaboratorIds = collaboratorIds == null ? Set.of() : Set.copyOf(collaboratorIds);
    tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
