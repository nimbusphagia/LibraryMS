package com.nimbusphagia.library.account.internal.service;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.account.internal.dto.AccountCreateRequest;
import com.nimbusphagia.library.account.internal.dto.AccountResponse;
import com.nimbusphagia.library.account.internal.dto.AccountUpdateRequest;
import com.nimbusphagia.library.account.internal.dto.RegistrationRequest;
import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.account.internal.entity.AccountRole;
import com.nimbusphagia.library.account.internal.repository.AccountRepository;
import com.nimbusphagia.library.exception.ErrorCodes;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ForbiddenException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.organization.AccessGuard;
import com.nimbusphagia.library.organization.OrganizationApi;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {
  private final AccountRepository accountRepository;
  private final AccountSessionService sessionService;
  private final PasswordEncoder passwordEncoder;
  private final AccessGuard accessGuard;
  private final OrganizationApi organizationApi;

  @Transactional(readOnly = true)
  public Page<AccountResponse> findAll(
      Long institutionId, AccountRole role, Boolean active, Pageable pageable) {
    AccountPrincipal actor = accessGuard.current();
    if (!actor.isOrganizationWide()) {
      institutionId = actor.getInstitutionId();
    }
    return accountRepository
        .search(actor.getOrganizationId(), institutionId, role, active, pageable)
        .map(AccountResponse::fromEntity);
  }

  @Transactional(readOnly = true)
  public AccountResponse findOne(Long id) {
    return AccountResponse.fromEntity(findVisible(id));
  }

  @Transactional
  public AccountResponse create(AccountCreateRequest request) {
    AccountPrincipal actor = accessGuard.current();
    requireCanManage(actor, request.role(), request.institutionId());
    requireUsernameAvailable(request.username());
    Account account =
        accountRepository.save(
            Account.builder()
                .organizationId(actor.getOrganizationId())
                .institutionId(request.institutionId())
                .username(request.username())
                .fullName(request.fullName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .mustChangePassword(true)
                .build());
    return AccountResponse.fromEntity(account);
  }

  @Transactional
  public AccountResponse edit(Long id, AccountUpdateRequest request) {
    AccountPrincipal actor = accessGuard.current();
    Account account = findManageable(actor, id);
    requireCanManage(actor, request.role(), request.institutionId());

    boolean scopeChanged =
        account.getRole() != request.role()
            || !Objects.equals(account.getInstitutionId(), request.institutionId());
    account.setFullName(request.fullName());
    account.setRole(request.role());
    account.setInstitutionId(request.institutionId());
    if (scopeChanged) {
      sessionService.invalidateAll(account.getUsername());
    }
    return AccountResponse.fromEntity(account);
  }

  @Transactional
  public AccountResponse deactivate(Long id) {
    Account account = findManageable(accessGuard.current(), id);
    account.setActive(false);
    sessionService.invalidateAll(account.getUsername());
    return AccountResponse.fromEntity(account);
  }

  @Transactional
  public AccountResponse activate(Long id) {
    Account account = findManageable(accessGuard.current(), id);
    account.setActive(true);
    return AccountResponse.fromEntity(account);
  }

  @Transactional
  public AccountResponse resetPassword(Long id, String newPassword) {
    Account account = findManageable(accessGuard.current(), id);
    account.setPasswordHash(passwordEncoder.encode(newPassword));
    account.setMustChangePassword(true);
    sessionService.invalidateAll(account.getUsername());
    return AccountResponse.fromEntity(account);
  }

  @Transactional
  public void changeOwnPassword(Long accountId, String currentPassword, String newPassword) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
      throw new BadRequestException("Current password is incorrect");
    }
    if (passwordEncoder.matches(newPassword, account.getPasswordHash())) {
      throw new BadRequestException("The new password must be different from the current one");
    }
    account.setPasswordHash(passwordEncoder.encode(newPassword));
    account.setMustChangePassword(false);
  }

  @Transactional
  public AccountResponse register(RegistrationRequest request) {
    requireUsernameAvailable(request.username());
    Long organizationId = organizationApi.createOrganization(request.organizationName());
    Account owner =
        accountRepository.save(
            Account.builder()
                .organizationId(organizationId)
                .username(request.username())
                .fullName(request.fullName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(AccountRole.OWNER)
                .build());
    return AccountResponse.fromEntity(owner);
  }

  // Helpers
  private Account findVisible(Long id) {
    AccountPrincipal actor = accessGuard.current();
    return accountRepository
        .findByIdAndOrganizationId(id, actor.getOrganizationId())
        .filter(
            a ->
                actor.isOrganizationWide()
                    || actor.getInstitutionId().equals(a.getInstitutionId()))
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
  }

  private Account findManageable(AccountPrincipal actor, Long id) {
    if (actor.getId().equals(id)) {
      throw new BadRequestException("You cannot manage your own account here");
    }
    Account account = findVisible(id);
    requireCanManage(actor, account.getRole(), account.getInstitutionId());
    return account;
  }

  // OWNER and ADMIN manage every non-owner account of the organization; a LIBRARIAN only the
  // STAFF of their institution. Owners are created by the bootstrap or by registration.
  private void requireCanManage(AccountPrincipal actor, AccountRole role, Long institutionId) {
    if (role == AccountRole.OWNER) {
      throw new ForbiddenException(ErrorCodes.REQUIRES_OWNER, "Owner accounts cannot be managed");
    }
    if (role.isOrganizationWide() != (institutionId == null)) {
      throw new BadRequestException(
          role.isOrganizationWide()
              ? role + " accounts cannot belong to an institution"
              : role + " accounts must belong to an institution");
    }
    if (institutionId != null) {
      accessGuard.requireInstitution(institutionId);
    }
    if (actor.isOrganizationWide()) {
      return;
    }
    boolean librarianOverOwnStaff =
        AccountRole.LIBRARIAN.name().equals(actor.getRole())
            && role == AccountRole.STAFF
            && actor.getInstitutionId().equals(institutionId);
    if (!librarianOverOwnStaff) {
      throw new ForbiddenException(
          ErrorCodes.REQUIRES_ADMIN, "Only an admin can manage this account");
    }
  }

  private void requireUsernameAvailable(String username) {
    if (accountRepository.existsByUsernameIgnoreCase(username)) {
      throw new ConflictException("Username already exists");
    }
  }
}
