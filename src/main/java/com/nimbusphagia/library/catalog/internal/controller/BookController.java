package com.nimbusphagia.library.catalog.internal.controller;

import com.nimbusphagia.library.catalog.internal.dto.book.BookFilter;
import com.nimbusphagia.library.catalog.internal.dto.book.BookRequest;
import com.nimbusphagia.library.catalog.internal.dto.book.BookResponse;
import com.nimbusphagia.library.catalog.internal.dto.book.BookSummaryResponse;
import com.nimbusphagia.library.catalog.internal.service.BookService;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Without an institution in the path it is the shared catalog, which the service keeps to admins
@RestController
@RequestMapping({"/api/books", "/api/institutions/{institutionId}/books"})
@RequiredArgsConstructor
class BookController {
  private final BookService bookService;

  @GetMapping
  Page<BookSummaryResponse> findAll(
      @PathVariable(required = false) Long institutionId, BookFilter filter, Pageable pageable) {
    return bookService.findAll(institutionId, filter, pageable);
  }

  @GetMapping("/{id}")
  BookResponse findOne(@PathVariable(required = false) Long institutionId, @PathVariable Long id) {
    return bookService.findOne(institutionId, id);
  }

  @PostMapping
  @StaffOnly
  @ResponseStatus(HttpStatus.CREATED)
  BookResponse create(
      @PathVariable(required = false) Long institutionId,
      @Valid @RequestBody BookRequest request) {
    return bookService.create(institutionId, request);
  }

  @PutMapping("/{id}")
  @StaffOnly
  BookResponse edit(
      @PathVariable(required = false) Long institutionId,
      @PathVariable Long id,
      @Valid @RequestBody BookRequest request) {
    return bookService.edit(institutionId, id, request);
  }

  @PatchMapping("/{id}/deactivate")
  @StaffOnly
  BookResponse deactivate(
      @PathVariable(required = false) Long institutionId, @PathVariable Long id) {
    return bookService.deactivate(institutionId, id);
  }

  @PatchMapping("/{id}/activate")
  @StaffOnly
  BookResponse activate(@PathVariable(required = false) Long institutionId, @PathVariable Long id) {
    return bookService.activate(institutionId, id);
  }
}
