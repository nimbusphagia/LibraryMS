package com.nimbusphagia.library.catalog.internal.controller;

import com.nimbusphagia.library.catalog.internal.dto.deweyTag.DeweyTagResponse;
import com.nimbusphagia.library.catalog.internal.service.DeweyTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Dewey classes are global reference data, read-only for every organization
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
}
