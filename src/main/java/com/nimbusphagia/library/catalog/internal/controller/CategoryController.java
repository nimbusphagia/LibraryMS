package com.nimbusphagia.library.catalog.internal.controller;

import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.dto.category.CategoryRequest;
import com.nimbusphagia.library.catalog.internal.dto.category.CategoryResponse;
import com.nimbusphagia.library.catalog.internal.service.CategoryService;
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
@RequestMapping({"/api/categories", "/api/institutions/{institutionId}/categories"})
@RequiredArgsConstructor
class CategoryController {
  private final CategoryService categoryService;

  @GetMapping
  Page<CategoryResponse> findAll(
      @PathVariable(required = false) Long institutionId,
      @RequestParam(defaultValue = "ALL") CatalogScope scope,
      @RequestParam(required = false) String search,
      Pageable pageable) {
    return categoryService.findAll(institutionId, scope, search, pageable);
  }

  @PostMapping
  @StaffOnly
  @ResponseStatus(HttpStatus.CREATED)
  CategoryResponse create(
      @PathVariable(required = false) Long institutionId,
      @Valid @RequestBody CategoryRequest request) {
    return categoryService.create(institutionId, request);
  }

  @PutMapping("/{id}")
  @StaffOnly
  CategoryResponse edit(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long id,
      @Valid @RequestBody CategoryRequest request) {
    return categoryService.edit(institutionId, id, request);
  }

  @DeleteMapping("/{id}")
  @LibrarianOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable(required = false) Long institutionId, @PathVariable Long id) {
    categoryService.delete(institutionId, id);
  }
}
