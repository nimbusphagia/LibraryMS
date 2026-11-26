package com.nimbusphagia.library.account.internal.dto;

import com.nimbusphagia.library.account.AccountPrincipal;

public record AccountMeResponse(
    Long id,
    String username,
    String fullName,
    String role,
    Long organizationId,
    Long institutionId,
    boolean mustChangePassword) {
  public static AccountMeResponse from(AccountPrincipal p) {
    return new AccountMeResponse(
        p.getId(),
        p.getUsername(),
        p.getFullName(),
        p.getRole(),
        p.getOrganizationId(),
        p.getInstitutionId(),
        p.isMustChangePassword());
  }
}
