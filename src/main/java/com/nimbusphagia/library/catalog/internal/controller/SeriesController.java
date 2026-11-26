package com.nimbusphagia.library.catalog.internal.controller;

import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.dto.series.SeriesRequest;
import com.nimbusphagia.library.catalog.internal.dto.series.SeriesResponse;
import com.nimbusphagia.library.catalog.internal.service.SeriesService;
import com.nimbusphagia.library.shared.security.LibrarianOnly;
import com.nimbusphagia.library.shared.security.StaffOnly;
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

// Without an institution in the path it is the shared catalog, which the service keeps to admins
@RestController
@RequestMapping({
  "/api/authors/{authorId}/series",
  "/api/institutions/{institutionId}/authors/{authorId}/series"
})
@RequiredArgsConstructor
class SeriesController {
  private final SeriesService seriesService;

  @GetMapping
  Page<SeriesResponse> findAll(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long authorId,
      @RequestParam(defaultValue = "ALL") CatalogScope scope,
      @RequestParam(required = false) String search,
      Pageable pageable) {
    return seriesService.findAll(institutionId, authorId, scope, search, pageable);
  }

  @PostMapping
  @StaffOnly
  @ResponseStatus(HttpStatus.CREATED)
  SeriesResponse create(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long authorId,
      @Valid @RequestBody SeriesRequest request) {
    return seriesService.create(institutionId, authorId, request);
  }

  @PutMapping("/{seriesId}")
  @StaffOnly
  SeriesResponse edit(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long authorId,
      @PathVariable Long seriesId,
      @Valid @RequestBody SeriesRequest request) {
    return seriesService.edit(institutionId, authorId, seriesId, request);
  }

  @DeleteMapping("/{seriesId}")
  @LibrarianOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long authorId,
      @PathVariable Long seriesId) {
    seriesService.delete(institutionId, authorId, seriesId);
  }
}
