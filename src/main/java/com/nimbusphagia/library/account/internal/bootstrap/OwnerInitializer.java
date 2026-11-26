package com.nimbusphagia.library.account.internal.bootstrap;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.account.internal.entity.AccountRole;
import com.nimbusphagia.library.account.internal.repository.AccountRepository;
import com.nimbusphagia.library.organization.OrganizationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class OwnerInitializer implements ApplicationRunner {
  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;
  private final OrganizationApi organizationApi;

  @Value("${app.bootstrap.owner.username:owner}")
  private String username;

  @Value("${app.bootstrap.owner.password:}")
  private String password;

  @Value("${app.bootstrap.organization.name:Main organization}")
  private String organizationName;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (accountRepository.existsByRole(AccountRole.OWNER)) {
      return;
    }
    if (password.isBlank()) {
      throw new IllegalStateException(
          "No owner account exists and app.bootstrap.owner.password is not set");
    }
    accountRepository.save(
        Account.builder()
            .organizationId(organizationApi.createOrganization(organizationName))
            .username(username)
            .passwordHash(passwordEncoder.encode(password))
            .fullName("Owner")
            .role(AccountRole.OWNER)
            .mustChangePassword(true)
            .build());
  }
}
