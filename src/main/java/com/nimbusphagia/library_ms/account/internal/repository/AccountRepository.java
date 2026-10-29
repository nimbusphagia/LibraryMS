package com.nimbusphagia.library_ms.account.internal.repository;

import com.nimbusphagia.library_ms.account.internal.entity.Account;
import com.nimbusphagia.library_ms.account.internal.entity.AccountRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {
  boolean existsByRole(AccountRole role);

  @Query("SELECT a FROM Account a WHERE LOWER(a.username) = LOWER(:username)")
  Optional<Account> findByUsernameIgnoreCase(@Param("username") String username);
}
