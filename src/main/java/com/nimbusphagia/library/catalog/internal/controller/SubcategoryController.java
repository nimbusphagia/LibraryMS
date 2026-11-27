package com.nimbusphagia.library.catalog.internal.controller;

import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.dto.subcategory.SubcategoryRequest;
import com.nimbusphagia.library.catalog.internal.dto.subcategory.SubcategoryResponse;
import com.nimbusphagia.library.catalog.internal.service.SubcategoryService;
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
  "/api/categories/{categoryId}/subcategories",
  "/api/institutions/{institutionId}/categories/{categoryId}/subcategories"
})
@RequiredArgsConstructor
class SubcategoryController {
  private final SubcategoryService subcategoryService;

  @GetMapping
  Page<SubcategoryResponse> findAll(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long categoryId,
      @RequestParam(defaultValue = "ALL") CatalogScope scope,
      @RequestParam(required = false) String search,
      Pageable pageable) {
    return subcategoryService.findAll(institutionId, categoryId, scope, search, pageable);
  }

  @PostMapping
  @StaffOnly
  @ResponseStatus(HttpStatus.CREATED)
  SubcategoryResponse create(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long categoryId,
      @Valid @RequestBody SubcategoryRequest request) {
    return subcategoryService.create(institutionId, categoryId, request);
  }

  @PutMapping("/{subcategoryId}")
  @StaffOnly
  SubcategoryResponse edit(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long categoryId,
      @PathVariable Long subcategoryId,
      @Valid @RequestBody SubcategoryRequest request) {
    return subcategoryService.edit(institutionId, categoryId, subcategoryId, request);
  }

  @DeleteMapping("/{subcategoryId}")
  @LibrarianOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long categoryId,
      @PathVariable Long subcategoryId) {
    subcategoryService.delete(institutionId, categoryId, subcategoryId);
  }
}
