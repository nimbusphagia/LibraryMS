package com.nimbusphagia.library_ms.catalog.internal.controller;

import com.nimbusphagia.library_ms.catalog.internal.dto.series.SeriesRequest;
import com.nimbusphagia.library_ms.catalog.internal.dto.series.SeriesResponse;
import com.nimbusphagia.library_ms.catalog.internal.service.SeriesService;
import com.nimbusphagia.library_ms.shared.security.AdminOnly;
import com.nimbusphagia.library_ms.shared.security.StaffOnly;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/authors/{authorId}/series")
@RequiredArgsConstructor
class SeriesController {
  private final SeriesService seriesService;

  @GetMapping
  Page<SeriesResponse> findAll(
      @PathVariable Long authorId,
      @RequestParam(required = false) String search,
      Pageable pageable) {
    return seriesService.findAll(authorId, search, pageable);
  }

  @PostMapping
  @StaffOnly
  @ResponseStatus(HttpStatus.CREATED)
  SeriesResponse create(@PathVariable Long authorId, @Valid @RequestBody SeriesRequest request) {
    return seriesService.create(authorId, request);
  }

  @PutMapping("/{seriesId}")
  @StaffOnly
  SeriesResponse edit(
      @PathVariable Long authorId,
      @PathVariable Long seriesId,
      @Valid @RequestBody SeriesRequest request) {
    return seriesService.edit(authorId, seriesId, request);
  }

  @DeleteMapping("/{seriesId}")
  @AdminOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable Long authorId, @PathVariable Long seriesId) {
    seriesService.delete(authorId, seriesId);
  }
}
