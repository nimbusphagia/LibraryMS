package com.nimbusphagia.library.inventory.internal.controller;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.inventory.internal.dto.copy.CopyConditionEventResponse;
import com.nimbusphagia.library.inventory.internal.dto.copy.CopyConditionRequest;
import com.nimbusphagia.library.inventory.internal.dto.copy.CopyFilter;
import com.nimbusphagia.library.inventory.internal.dto.copy.CopyMoveRequest;
import com.nimbusphagia.library.inventory.internal.dto.copy.CopyResponse;
import com.nimbusphagia.library.inventory.internal.service.CopyService;
import com.nimbusphagia.library.shared.security.LibrarianOnly;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/libraries/{libraryId}/copies")
@StaffOnly
@RequiredArgsConstructor
class CopyController {
  private final CopyService copyService;

  @GetMapping
  Page<CopyResponse> findAll(
      @PathVariable Long libraryId,
      CopyFilter filter,
      @PageableDefault(
              size = 20,
              sort = {"book.id", "iteration"})
          Pageable pageable) {
    return copyService.findAll(libraryId, filter, pageable);
  }

  @GetMapping("/{copyId}")
  CopyResponse findOne(@PathVariable Long libraryId, @PathVariable Long copyId) {
    return copyService.findOne(libraryId, copyId);
  }

  @PostMapping("/move")
  List<CopyResponse> move(
      @PathVariable Long libraryId, @Valid @RequestBody CopyMoveRequest request) {
    return copyService.move(libraryId, request);
  }

  @PostMapping("/{copyId}/damage")
  @LibrarianOnly
  CopyResponse damage(
      @PathVariable Long libraryId,
      @PathVariable Long copyId,
      @Valid @RequestBody CopyConditionRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return copyService.damage(libraryId, copyId, request.reason(), principal.getId());
  }

  @PostMapping("/{copyId}/repair")
  @LibrarianOnly
  CopyResponse repair(
      @PathVariable Long libraryId,
      @PathVariable Long copyId,
      @Valid @RequestBody CopyConditionRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return copyService.repair(libraryId, copyId, request.reason(), principal.getId());
  }

  @GetMapping("/{copyId}/condition-events")
  List<CopyConditionEventResponse> conditionHistory(
      @PathVariable Long libraryId, @PathVariable Long copyId) {
    return copyService.conditionHistory(libraryId, copyId);
  }
}
