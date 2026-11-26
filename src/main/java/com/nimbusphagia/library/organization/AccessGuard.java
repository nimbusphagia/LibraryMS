package com.nimbusphagia.library.organization;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.exception.custom.UnauthorizedException;
import com.nimbusphagia.library.organization.internal.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Decides which data the current account may reach. Resources outside its scope answer 404, so
 * another organization's ids are indistinguishable from ids that do not exist.
 */
@Component
@RequiredArgsConstructor
public class AccessGuard {
  private final InstitutionRepository institutionRepository;

  public AccountPrincipal current() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof AccountPrincipal principal) {
      return principal;
    }
    throw new UnauthorizedException("Not authenticated");
  }

  public void requireLibrary(Long libraryId) {
    AccountPrincipal principal = current();
    institutionRepository
        .findScopeOfLibrary(libraryId)
        .filter(s -> canReach(principal, s.getOrganizationId(), s.getInstitutionId()))
        .orElseThrow(() -> new ResourceNotFoundException("Library not found"));
  }

  public void requireInstitution(Long institutionId) {
    AccountPrincipal principal = current();
    institutionRepository
        .findById(institutionId)
        .filter(i -> canReach(principal, i.getOrganizationId(), i.getId()))
        .orElseThrow(() -> new ResourceNotFoundException("Institution not found"));
  }

  private static boolean canReach(
      AccountPrincipal principal, Long organizationId, Long institutionId) {
    return principal.getOrganizationId().equals(organizationId)
        && (principal.isOrganizationWide() || principal.getInstitutionId().equals(institutionId));
  }
}
