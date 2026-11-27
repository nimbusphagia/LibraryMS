package com.nimbusphagia.library.catalog.internal.dto.book;

import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import java.util.Set;

public record BookFilter(
    String search,
    Long personId, // any
    Long authorId,
    Long illustratorId,
    Long collaboratorId,
    Long publisherId,
    Long collectionId,
    Long seriesId,
    Long categoryId,
    Long subcategoryId,
    String dewey, // class, division or full number: matches by prefix
    Set<Long> tagIds,
    String country,
    Boolean active,
    CatalogScope scope) {

  public BookFilter {
    scope = scope == null ? CatalogScope.ALL : scope;
    search = search == null || search.isBlank() ? null : search.strip();
    dewey = dewey == null || dewey.isBlank() ? null : dewey.strip();
    country = country == null || country.isBlank() ? null : country.strip().toUpperCase();
    tagIds = tagIds == null ? Set.of() : Set.copyOf(tagIds);
  }
}
