package com.nimbusphagia.library_ms.catalog.internal.controller;

import com.nimbusphagia.library_ms.catalog.internal.dto.publisher.PublisherRequest;
import com.nimbusphagia.library_ms.catalog.internal.dto.publisher.PublisherResponse;
import com.nimbusphagia.library_ms.catalog.internal.service.PublisherService;
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
@RequestMapping("/api/publishers")
@RequiredArgsConstructor
class PublisherController {
  private final PublisherService publisherService;

  @GetMapping
  Page<PublisherResponse> findAll(
      @RequestParam(required = false) String search, Pageable pageable) {
    return publisherService.findAll(search, pageable);
  }

  @PostMapping
  @StaffOnly
  @ResponseStatus(HttpStatus.CREATED)
  PublisherResponse create(@Valid @RequestBody PublisherRequest request) {
    return publisherService.create(request);
  }

  @PutMapping("/{id}")
  @StaffOnly
  PublisherResponse edit(@PathVariable Long id, @Valid @RequestBody PublisherRequest request) {
    return publisherService.edit(id, request);
  }

  @DeleteMapping("/{id}")
  @AdminOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable Long id) {
    publisherService.delete(id);
  }
}
