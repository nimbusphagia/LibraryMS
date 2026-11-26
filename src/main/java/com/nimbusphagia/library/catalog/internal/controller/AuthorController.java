package com.nimbusphagia.library.catalog.internal.controller;

import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.dto.author.AuthorRequest;
import com.nimbusphagia.library.catalog.internal.dto.author.AuthorResponse;
import com.nimbusphagia.library.catalog.internal.service.AuthorService;
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
@RequestMapping({"/api/authors", "/api/institutions/{institutionId}/authors"})
@RequiredArgsConstructor
class AuthorController {
  private final AuthorService authorService;

  @GetMapping
  Page<AuthorResponse> findAll(
      @PathVariable(required = false) Long institutionId,
      @RequestParam(defaultValue = "ALL") CatalogScope scope,
      @RequestParam(required = false) String search,
      Pageable pageable) {
    return authorService.findAll(institutionId, scope, search, pageable);
  }

  @PostMapping
  @StaffOnly
  @ResponseStatus(HttpStatus.CREATED)
  AuthorResponse create(
      @PathVariable(required = false) Long institutionId,
      @Valid @RequestBody AuthorRequest request) {
    return authorService.create(institutionId, request);
  }

  @PutMapping("/{id}")
  @StaffOnly
  AuthorResponse edit(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long id,
      @Valid @RequestBody AuthorRequest request) {
    return authorService.edit(institutionId, id, request);
  }

  @DeleteMapping("/{id}")
  @LibrarianOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable(required = false) Long institutionId, @PathVariable Long id) {
    authorService.delete(institutionId, id);
  }
}
