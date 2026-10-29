package com.nimbusphagia.library_ms.account.internal.bootstrap;

import com.nimbusphagia.library_ms.account.internal.entity.Account;
import com.nimbusphagia.library_ms.account.internal.entity.AccountRole;
import com.nimbusphagia.library_ms.account.internal.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OwnerInitializer implements ApplicationRunner {
  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.bootstrap.owner.username:owner}")
  private String username;

  @Value("${app.bootstrap.owner.password:}")
  private String password;

  @Override
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
            .username(username)
            .passwordHash(passwordEncoder.encode(password))
            .fullName("Owner")
            .role(AccountRole.OWNER)
            .build());
  }
}
