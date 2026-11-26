package com.nimbusphagia.library.catalog.internal.service;

import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.anyContains;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.attributeIs;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.idNot;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.textEquals;

import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.dto.series.SeriesRequest;
import com.nimbusphagia.library.catalog.internal.dto.series.SeriesResponse;
import com.nimbusphagia.library.catalog.internal.entity.Author;
import com.nimbusphagia.library.catalog.internal.entity.Series;
import com.nimbusphagia.library.catalog.internal.repository.AuthorRepository;
import com.nimbusphagia.library.catalog.internal.repository.SeriesRepository;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeriesService {
  private final SeriesRepository seriesRepository;
  private final AuthorRepository authorRepository;
  private final CatalogAccess access;

  @Transactional
  public SeriesResponse create(Long institutionId, Long authorId, SeriesRequest request) {
    Author author = findAuthor(institutionId, authorId);
    Series series = access.stamp(request.toEntity(author), institutionId);
    requireUniqueName(institutionId, authorId, request.name(), null);
    return SeriesResponse.fromEntity(seriesRepository.save(series));
  }

  @Transactional
  public SeriesResponse edit(
      Long institutionId, Long authorId, Long seriesId, SeriesRequest request) {
    Series series = findOwned(institutionId, authorId, seriesId);
    requireUniqueName(institutionId, authorId, request.name(), seriesId);
    request.replace(series);
    return SeriesResponse.fromEntity(series);
  }

  @Transactional(readOnly = true)
  public Page<SeriesResponse> findAll(
      Long institutionId, Long authorId, CatalogScope scope, String search, Pageable pageable) {
    findAuthor(institutionId, authorId);
    return seriesRepository
        .findAll(
            access
                .<Series>in(institutionId, scope)
                .and(attributeIs("author", authorId))
                .and(anyContains(search, "name")),
            pageable)
        .map(SeriesResponse::fromEntity);
  }

  @Transactional
  public void delete(Long institutionId, Long authorId, Long seriesId) {
    Series series = findOwned(institutionId, authorId, seriesId);
    if (seriesRepository.isReferenced(seriesId)) {
      throw new ConflictException("Series has books and cannot be deleted");
    }
    seriesRepository.delete(series);
  }

  // Helper
  private Author findAuthor(Long institutionId, Long authorId) {
    return access
        .visible(authorRepository.findById(authorId), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
  }

  private Series findOwned(Long institutionId, Long authorId, Long seriesId) {
    access.requireWriter(institutionId);
    return access
        .owned(seriesRepository.findByIdAndAuthorId(seriesId, authorId), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Series not found"));
  }

  private void requireUniqueName(Long institutionId, Long authorId, String name, Long id) {
    if (seriesRepository.exists(
        access
            .<Series>visible(institutionId)
            .and(attributeIs("author", authorId))
            .and(textEquals("name", name))
            .and(idNot(id)))) {
      throw new ConflictException("Series name already exists for this author");
    }
  }
}
