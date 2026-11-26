package com.nimbusphagia.library.account.internal.controller;

import com.nimbusphagia.library.account.internal.dto.AccountCreateRequest;
import com.nimbusphagia.library.account.internal.dto.AccountResponse;
import com.nimbusphagia.library.account.internal.dto.AccountUpdateRequest;
import com.nimbusphagia.library.account.internal.dto.PasswordResetRequest;
import com.nimbusphagia.library.account.internal.entity.AccountRole;
import com.nimbusphagia.library.account.internal.service.AccountService;
import com.nimbusphagia.library.shared.security.LibrarianOnly;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@LibrarianOnly
@RequiredArgsConstructor
class AccountController {
  private final AccountService accountService;

  @GetMapping
  Page<AccountResponse> findAll(
      @RequestParam(required = false) Long institutionId,
      @RequestParam(required = false) AccountRole role,
      @RequestParam(required = false) Boolean active,
      @PageableDefault(size = 20, sort = "username") Pageable pageable) {
    return accountService.findAll(institutionId, role, active, pageable);
  }

  @GetMapping("/{accountId}")
  AccountResponse findOne(@PathVariable Long accountId) {
    return accountService.findOne(accountId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  AccountResponse create(@Valid @RequestBody AccountCreateRequest request) {
    return accountService.create(request);
  }

  @PutMapping("/{accountId}")
  AccountResponse edit(
      @PathVariable Long accountId, @Valid @RequestBody AccountUpdateRequest request) {
    return accountService.edit(accountId, request);
  }

  @PatchMapping("/{accountId}/deactivate")
  AccountResponse deactivate(@PathVariable Long accountId) {
    return accountService.deactivate(accountId);
  }

  @PatchMapping("/{accountId}/activate")
  AccountResponse activate(@PathVariable Long accountId) {
    return accountService.activate(accountId);
  }

  @PostMapping("/{accountId}/password-reset")
  AccountResponse resetPassword(
      @PathVariable Long accountId, @Valid @RequestBody PasswordResetRequest request) {
    return accountService.resetPassword(accountId, request.newPassword());
  }
}
