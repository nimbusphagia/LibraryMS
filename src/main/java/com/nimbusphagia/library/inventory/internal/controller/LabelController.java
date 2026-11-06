package com.nimbusphagia.library.inventory.internal.controller;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelCustomizeRequest;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelParts;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelRevisionResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelSuggestion;
import com.nimbusphagia.library.inventory.internal.dto.label.MarkPrintedRequest;
import com.nimbusphagia.library.inventory.internal.service.LabelService;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api")
@StaffOnly
@RequiredArgsConstructor
class LabelController {
  private final LabelService labelService;

  @GetMapping("/libraries/{libraryId}/books/{bookId}/label-suggestion")
  LabelSuggestion suggest(
      @PathVariable Long libraryId,
      @PathVariable Long bookId,
      @RequestParam(required = false) Integer copyNumber) {
    return labelService.suggest(libraryId, bookId, copyNumber);
  }

  @PostMapping("/copies/{copyId}/label")
  @ResponseStatus(HttpStatus.CREATED)
  LabelResponse create(
      @PathVariable Long copyId,
      @Valid @RequestBody LabelParts parts,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return labelService.createForExistingCopy(copyId, parts, principal.getId());
  }

  @PutMapping("/copies/{copyId}/label")
  LabelResponse customize(
      @PathVariable Long copyId,
      @Valid @RequestBody LabelCustomizeRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return labelService.customize(copyId, request, principal.getId());
  }

  @GetMapping("/copies/{copyId}/label")
  LabelResponse get(@PathVariable Long copyId) {
    return labelService.getByCopyId(copyId);
  }

  @GetMapping("/copies/{copyId}/label/history")
  List<LabelRevisionResponse> history(@PathVariable Long copyId) {
    return labelService.getHistory(copyId);
  }

  @GetMapping("/libraries/{libraryId}/labels/pending-print")
  List<LabelResponse> pendingPrint(@PathVariable Long libraryId) {
    return labelService.findPendingPrint(libraryId);
  }

  @PostMapping("/labels/mark-printed")
  List<LabelResponse> markPrinted(
      @Valid @RequestBody MarkPrintedRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return labelService.markPrinted(request.labelIds(), principal.getId());
  }
}
