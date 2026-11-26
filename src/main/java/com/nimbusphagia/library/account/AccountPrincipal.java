package com.nimbusphagia.library.account;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AccountPrincipal implements UserDetails, CredentialsContainer {
  private static final long serialVersionUID = 2L;

  private final Long id;
  private final String username;
  private final String fullName;
  private final String role;
  private final Long organizationId;
  private final Long institutionId;
  private final boolean active;
  private final boolean mustChangePassword;
  private String passwordHash;

  public AccountPrincipal(
      Long id,
      String username,
      String fullName,
      String role,
      Long organizationId,
      Long institutionId,
      boolean active,
      boolean mustChangePassword,
      String passwordHash) {
    this.id = id;
    this.username = username;
    this.fullName = fullName;
    this.role = role;
    this.organizationId = organizationId;
    this.institutionId = institutionId;
    this.active = active;
    this.mustChangePassword = mustChangePassword;
    this.passwordHash = passwordHash;
  }

  public Long getId() {
    return id;
  }

  public String getFullName() {
    return fullName;
  }

  public String getRole() {
    return role;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public Long getInstitutionId() {
    return institutionId;
  }

  public boolean isOrganizationWide() {
    return institutionId == null;
  }

  public boolean isMustChangePassword() {
    return mustChangePassword;
  }

  public AccountPrincipal withPasswordChanged() {
    return new AccountPrincipal(
        id, username, fullName, role, organizationId, institutionId, active, false, null);
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Override
  public void eraseCredentials() {
    this.passwordHash = null;
  }
}
