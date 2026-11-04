package com.nimbusphagia.library.catalog.internal.service;

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

  @Transactional
  public SeriesResponse create(Long authorId, SeriesRequest request) {
    Author author = findAuthor(authorId);
    if (seriesRepository.existsByAuthorAndName(authorId, request.name())) {
      throw new ConflictException("Series name already exists for this author");
    }
    Series series = seriesRepository.save(request.toEntity(author));
    return SeriesResponse.fromEntity(series);
  }

  @Transactional
  public SeriesResponse edit(Long authorId, Long seriesId, SeriesRequest request) {
    Series series = findSeries(authorId, seriesId);
    if (seriesRepository.existsByAuthorAndNameAndIdNot(authorId, request.name(), seriesId)) {
      throw new ConflictException("Series name already exists for this author");
    }
    request.replace(series);
    return SeriesResponse.fromEntity(series);
  }

  @Transactional(readOnly = true)
  public Page<SeriesResponse> findAll(Long authorId, String search, Pageable pageable) {
    if (!authorRepository.existsById(authorId)) {
      throw new ResourceNotFoundException("Author not found");
    }
    Page<Series> page =
        (search == null || search.isBlank())
            ? seriesRepository.findByAuthorId(authorId, pageable)
            : seriesRepository.findByAuthorIdAndNameContainingIgnoreCase(
                authorId, search.trim(), pageable);
    return page.map(SeriesResponse::fromEntity);
  }

  @Transactional
  public void delete(Long authorId, Long seriesId) {
    Series series = findSeries(authorId, seriesId);
    if (seriesRepository.isReferenced(seriesId)) {
      throw new ConflictException("Series has books and cannot be deleted");
    }
    seriesRepository.delete(series);
  }

  // Helper
  private Author findAuthor(Long authorId) {
    return authorRepository
        .findById(authorId)
        .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
  }

  private Series findSeries(Long authorId, Long seriesId) {
    return seriesRepository
        .findByIdAndAuthorId(seriesId, authorId)
        .orElseThrow(() -> new ResourceNotFoundException("Series not found"));
  }
}
