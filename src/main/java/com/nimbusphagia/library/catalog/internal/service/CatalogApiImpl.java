package com.nimbusphagia.library.catalog.internal.service;

import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.catalog.CatalogApi;
import com.nimbusphagia.library.catalog.internal.repository.BookRepository;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogApiImpl implements CatalogApi {
  private final BookRepository bookRepository;

  @Override
  @Transactional(readOnly = true)
  public BookLabelData getBookLabelData(Long bookId) {
    return getBookLabelData(Set.of(bookId)).values().stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public Map<Long, BookLabelData> getBookLabelData(Set<Long> bookIds) {
    if (bookIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return bookRepository.findLabelDataByIdIn(bookIds).stream()
        .collect(Collectors.toMap(BookLabelData::bookId, Function.identity()));
  }
}
