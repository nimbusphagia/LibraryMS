package com.nimbusphagia.library_ms.catalog.internal.controller;

import com.nimbusphagia.library_ms.catalog.internal.dto.deweyTag.DeweyTagRequest;
import com.nimbusphagia.library_ms.catalog.internal.dto.deweyTag.DeweyTagResponse;
import com.nimbusphagia.library_ms.catalog.internal.service.DeweyTagService;
import com.nimbusphagia.library_ms.shared.security.AdminOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dewey-tags")
@RequiredArgsConstructor
class DeweyTagController {
  private final DeweyTagService deweyTagService;

  @GetMapping
  Page<DeweyTagResponse> findAll(@RequestParam(required = false) String search, Pageable pageable) {
    return deweyTagService.findAll(search, pageable);
  }

  @GetMapping("/{id}")
  DeweyTagResponse findOne(@PathVariable Long id) {
    return deweyTagService.findOne(id);
  }

  @PutMapping("/{id}")
  @AdminOnly
  DeweyTagResponse edit(@PathVariable Long id, @RequestBody DeweyTagRequest request) {
    return deweyTagService.edit(id, request);
  }
}
