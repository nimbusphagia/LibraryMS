package com.nimbusphagia.library.account.internal.dto;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.account.internal.entity.AccountRole;

public record AccountResponse(
    Long id,
    String username,
    String fullName,
    AccountRole role,
    Long institutionId,
    boolean active,
    boolean mustChangePassword) {
  public static AccountResponse fromEntity(Account a) {
    return new AccountResponse(
        a.getId(),
        a.getUsername(),
        a.getFullName(),
        a.getRole(),
        a.getInstitutionId(),
        a.isActive(),
        a.isMustChangePassword());
  }
}
