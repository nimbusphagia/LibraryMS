package com.nimbusphagia.library_ms.catalog.internal.controller;

import com.nimbusphagia.library_ms.catalog.internal.dto.book.BookFilter;
import com.nimbusphagia.library_ms.catalog.internal.dto.book.BookRequest;
import com.nimbusphagia.library_ms.catalog.internal.dto.book.BookResponse;
import com.nimbusphagia.library_ms.catalog.internal.dto.book.BookSummaryResponse;
import com.nimbusphagia.library_ms.catalog.internal.service.BookService;
import com.nimbusphagia.library_ms.shared.security.StaffOnly;
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

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
class BookController {
  private final BookService bookService;

  @GetMapping
  Page<BookSummaryResponse> findAll(BookFilter filter, Pageable pageable) {
    return bookService.findAll(filter, pageable);
  }

  @GetMapping("/{id}")
  BookResponse findOne(@PathVariable Long id) {
    return bookService.findOne(id);
  }

  @PostMapping
  @StaffOnly
  @ResponseStatus(HttpStatus.CREATED)
  BookResponse create(@Valid @RequestBody BookRequest request) {
    return bookService.create(request);
  }

  @PutMapping("/{id}")
  @StaffOnly
  BookResponse edit(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
    return bookService.edit(id, request);
  }

  @PatchMapping("/{id}/deactivate")
  @StaffOnly
  BookResponse deactivate(@PathVariable Long id) {
    return bookService.deactivate(id);
  }

  @PatchMapping("/{id}/activate")
  @StaffOnly
  BookResponse activate(@PathVariable Long id) {
    return bookService.activate(id);
  }
}
