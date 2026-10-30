package com.nimbusphagia.library_ms.catalog.internal.controller;

import com.nimbusphagia.library_ms.catalog.internal.dto.collection.CollectionRequest;
import com.nimbusphagia.library_ms.catalog.internal.dto.collection.CollectionResponse;
import com.nimbusphagia.library_ms.catalog.internal.service.CollectionService;
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
@RequestMapping("/api/publishers/{publisherId}/collections")
@RequiredArgsConstructor
class CollectionController {
  private final CollectionService collectionService;

  @GetMapping
  Page<CollectionResponse> findAll(
      @PathVariable Long publisherId,
      @RequestParam(required = false) String search,
      Pageable pageable) {
    return collectionService.findAll(publisherId, search, pageable);
  }

  @PostMapping
  @StaffOnly
  @ResponseStatus(HttpStatus.CREATED)
  CollectionResponse create(
      @PathVariable Long publisherId, @Valid @RequestBody CollectionRequest request) {
    return collectionService.create(publisherId, request);
  }

  @PutMapping("/{collectionId}")
  @StaffOnly
  CollectionResponse edit(
      @PathVariable Long publisherId,
      @PathVariable Long collectionId,
      @Valid @RequestBody CollectionRequest request) {
    return collectionService.edit(publisherId, collectionId, request);
  }

  @DeleteMapping("/{collectionId}")
  @AdminOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable Long publisherId, @PathVariable Long collectionId) {
    collectionService.delete(publisherId, collectionId);
  }
}
