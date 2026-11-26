package com.nimbusphagia.library.catalog.internal.controller;

import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.dto.collection.CollectionRequest;
import com.nimbusphagia.library.catalog.internal.dto.collection.CollectionResponse;
import com.nimbusphagia.library.catalog.internal.service.CollectionService;
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
  "/api/publishers/{publisherId}/collections",
  "/api/institutions/{institutionId}/publishers/{publisherId}/collections"
})
@RequiredArgsConstructor
class CollectionController {
  private final CollectionService collectionService;

  @GetMapping
  Page<CollectionResponse> findAll(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long publisherId,
      @RequestParam(defaultValue = "ALL") CatalogScope scope,
      @RequestParam(required = false) String search,
      Pageable pageable) {
    return collectionService.findAll(institutionId, publisherId, scope, search, pageable);
  }

  @PostMapping
  @StaffOnly
  @ResponseStatus(HttpStatus.CREATED)
  CollectionResponse create(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long publisherId,
      @Valid @RequestBody CollectionRequest request) {
    return collectionService.create(institutionId, publisherId, request);
  }

  @PutMapping("/{collectionId}")
  @StaffOnly
  CollectionResponse edit(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long publisherId,
      @PathVariable Long collectionId,
      @Valid @RequestBody CollectionRequest request) {
    return collectionService.edit(institutionId, publisherId, collectionId, request);
  }

  @DeleteMapping("/{collectionId}")
  @LibrarianOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long publisherId,
      @PathVariable Long collectionId) {
    collectionService.delete(institutionId, publisherId, collectionId);
  }
}
