package com.nimbusphagia.library_ms.catalog.internal.dto.book;

import java.util.Set;

public record BookFilter(
    String search,
    Long personId, // any
    Long authorId,
    Long illustratorId,
    Long collaboratorId,
    Long publisherId,
    Long collectionId,
    Long deweyId,
    Set<Long> tagIds,
    String country,
    Boolean active) {

  public BookFilter {
    search = search == null || search.isBlank() ? null : search.strip();
    country = country == null || country.isBlank() ? null : country.strip().toUpperCase();
    tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
  }
}
