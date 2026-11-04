package com.nimbusphagia.library.catalog.internal.controller;

import com.nimbusphagia.library.catalog.internal.dto.category.CategoryRequest;
import com.nimbusphagia.library.catalog.internal.dto.category.CategoryResponse;
import com.nimbusphagia.library.catalog.internal.service.CategoryService;
import com.nimbusphagia.library.shared.security.AdminOnly;
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

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
class CategoryController {
  private final CategoryService categoryService;

  @GetMapping
  Page<CategoryResponse> findAll(@RequestParam(required = false) String search, Pageable pageable) {
    return categoryService.findAll(search, pageable);
  }

  @PostMapping
  @StaffOnly
  @ResponseStatus(HttpStatus.CREATED)
  CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
    return categoryService.create(request);
  }

  @PutMapping("/{id}")
  @StaffOnly
  CategoryResponse edit(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
    return categoryService.edit(id, request);
  }

  @DeleteMapping("/{id}")
  @AdminOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable Long id) {
    categoryService.delete(id);
  }
}
