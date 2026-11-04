package com.nimbusphagia.library.account.internal.dto;

import com.nimbusphagia.library.account.AccountPrincipal;

public record AccountMeResponse(Long id, String username, String fullName, String role) {
  public static AccountMeResponse from(AccountPrincipal p) {
    return new AccountMeResponse(p.getId(), p.getUsername(), p.getFullName(), p.getRole());
  }
}
