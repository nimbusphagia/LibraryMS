package com.nimbusphagia.library_ms.catalog.internal.controller;

import com.nimbusphagia.library_ms.catalog.internal.dto.tag.TagRequest;
import com.nimbusphagia.library_ms.catalog.internal.dto.tag.TagResponse;
import com.nimbusphagia.library_ms.catalog.internal.service.TagService;
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
@RequestMapping("/api/tags")
@RequiredArgsConstructor
class TagController {
  private final TagService tagService;

  @GetMapping
  Page<TagResponse> findAll(@RequestParam(required = false) String search, Pageable pageable) {
    return tagService.findAll(search, pageable);
  }

  @PostMapping
  @StaffOnly
  @ResponseStatus(HttpStatus.CREATED)
  TagResponse create(@Valid @RequestBody TagRequest request) {
    return tagService.create(request);
  }

  @PutMapping("/{id}")
  @StaffOnly
  TagResponse edit(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
    return tagService.edit(id, request);
  }

  @DeleteMapping("/{id}")
  @AdminOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable Long id) {
    tagService.delete(id);
  }
}
