package com.nimbusphagia.library.inventory.internal.controller;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.inventory.internal.dto.label.BookLabelResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.BookLabelRevisionResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelCustomizeRequest;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelRequest;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestion;
import com.nimbusphagia.library.inventory.internal.dto.label.MarkPrintedRequest;
import com.nimbusphagia.library.inventory.internal.service.LabelService;
import com.nimbusphagia.library.shared.security.LibrarianOnly;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/libraries/{libraryId}")
@StaffOnly
@RequiredArgsConstructor
class LabelController {
  private final LabelService labelService;

  @GetMapping("/books/{bookId}/label-suggestion")
  LabelSuggestion suggest(
      @PathVariable Long libraryId,
      @PathVariable Long bookId,
      @RequestParam(required = false) Integer copyNumber) {
    return labelService.suggest(libraryId, bookId, copyNumber);
  }

  @PostMapping("/copies/{copyId}/label")
  @ResponseStatus(HttpStatus.CREATED)
  LabelResponse create(
      @PathVariable Long libraryId,
      @PathVariable Long copyId,
      @Valid @RequestBody LabelRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return labelService.createForExistingCopy(
        libraryId, copyId, request.parts(), principal.getId());
  }

  @GetMapping("/copies/{copyId}/label")
  LabelResponse get(@PathVariable Long libraryId, @PathVariable Long copyId) {
    return labelService.getByCopyId(libraryId, copyId);
  }

  // The book's label in this library, which all its copies follow

  @GetMapping("/books/{bookId}/label")
  BookLabelResponse getPattern(@PathVariable Long libraryId, @PathVariable Long bookId) {
    return labelService.getPattern(libraryId, bookId);
  }

  @PutMapping("/books/{bookId}/label")
  @LibrarianOnly
  BookLabelResponse customize(
      @PathVariable Long libraryId,
      @PathVariable Long bookId,
      @Valid @RequestBody LabelCustomizeRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return labelService.customize(libraryId, bookId, request, principal.getId());
  }

  @PostMapping("/books/{bookId}/label/refresh")
  BookLabelResponse refresh(
      @PathVariable Long libraryId,
      @PathVariable Long bookId,
      @Valid @RequestBody(required = false) LabelRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    Map<LabelPart, String> parts = request == null ? Map.of() : request.parts();
    return labelService.refresh(libraryId, bookId, parts, principal.getId());
  }

  @GetMapping("/books/{bookId}/label/history")
  List<BookLabelRevisionResponse> history(
      @PathVariable Long libraryId, @PathVariable Long bookId) {
    return labelService.getHistory(libraryId, bookId);
  }

  @GetMapping("/labels/pending-print")
  List<LabelResponse> pendingPrint(@PathVariable Long libraryId) {
    return labelService.findPendingPrint(libraryId);
  }

  @PostMapping("/labels/mark-printed")
  List<LabelResponse> markPrinted(
      @PathVariable Long libraryId,
      @Valid @RequestBody MarkPrintedRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return labelService.markPrinted(libraryId, request.labelIds(), principal.getId());
  }
}
