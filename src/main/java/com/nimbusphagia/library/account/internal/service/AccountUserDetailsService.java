package com.nimbusphagia.library.account.internal.service;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.account.internal.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AccountUserDetailsService implements UserDetailsService {
  private final AccountRepository accountRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) {
    Account account =
        accountRepository
            .findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    return new AccountPrincipal(
        account.getId(),
        account.getUsername(),
        account.getFullName(),
        account.getRole().name(),
        account.isActive(),
        account.getPasswordHash());
  }
}
