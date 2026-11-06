package com.nimbusphagia.library.inventory.internal.controller;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockEntryRequest;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockEntryResponse;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockExitRequest;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockExitResponse;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockMovementDetailResponse;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockMovementFilter;
import com.nimbusphagia.library.inventory.internal.dto.stock.StockMovementSummaryResponse;
import com.nimbusphagia.library.inventory.internal.service.StockMovementQueryService;
import com.nimbusphagia.library.inventory.internal.service.StockMovementService;
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
@RequestMapping("/api/libraries/{libraryId}")
@StaffOnly
@RequiredArgsConstructor
class StockMovementController {
  private final StockMovementService stockMovementService;
  private final StockMovementQueryService queryService;

  @GetMapping("/stock-movements")
  Page<StockMovementSummaryResponse> findAll(
      @PathVariable Long libraryId,
      StockMovementFilter filter,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return queryService.findAll(libraryId, filter, pageable);
  }

  @PostMapping("/stock-entries")
  @ResponseStatus(HttpStatus.CREATED)
  StockEntryResponse registerEntry(
      @PathVariable Long libraryId,
      @Valid @RequestBody StockEntryRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return stockMovementService.registerEntry(libraryId, request, principal.getId());
  }

  @PostMapping("/stock-exits")
  @ResponseStatus(HttpStatus.CREATED)
  StockExitResponse registerExit(
      @PathVariable Long libraryId,
      @Valid @RequestBody StockExitRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return stockMovementService.registerExit(libraryId, request, principal.getId());
  }

  @GetMapping("/stock-movements/{movementId}")
  StockMovementDetailResponse findOne(@PathVariable Long libraryId, @PathVariable Long movementId) {
    return queryService.findOne(libraryId, movementId);
  }
}
