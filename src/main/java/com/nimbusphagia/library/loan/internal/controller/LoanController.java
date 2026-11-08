package com.nimbusphagia.library.loan.internal.controller;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanDetailResponse;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanFilter;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanRequest;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanResolutionRequest;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanResponse;
import com.nimbusphagia.library.loan.internal.dto.loan.LoanSummaryResponse;
import com.nimbusphagia.library.loan.internal.service.LoanQueryService;
import com.nimbusphagia.library.loan.internal.service.LoanResolutionService;
import com.nimbusphagia.library.loan.internal.service.LoanService;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/libraries/{libraryId}/loans")
@StaffOnly
@RequiredArgsConstructor
class LoanController {
  private final LoanService loanService;
  private final LoanQueryService queryService;
  private final LoanResolutionService resolutionService;

  @GetMapping
  Page<LoanSummaryResponse> findAll(
      @PathVariable Long libraryId,
      LoanFilter filter,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return queryService.findAll(libraryId, filter, pageable);
  }

  @GetMapping("/{loanId}")
  LoanDetailResponse findOne(@PathVariable Long libraryId, @PathVariable Long loanId) {
    return queryService.findOne(libraryId, loanId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  LoanResponse create(
      @PathVariable Long libraryId,
      @Valid @RequestBody LoanRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return loanService.create(libraryId, request, principal.getId());
  }

  @PostMapping("/{loanId}/resolutions")
  LoanDetailResponse resolve(
      @PathVariable Long libraryId,
      @PathVariable Long loanId,
      @Valid @RequestBody LoanResolutionRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return resolutionService.resolve(libraryId, loanId, request, principal.getId());
  }
}
