package com.nimbusphagia.library.account.internal.repository;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.account.internal.entity.AccountRole;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {
  boolean existsByRole(AccountRole role);

  @Query("SELECT COUNT(a) > 0 FROM Account a WHERE LOWER(a.username) = LOWER(:username)")
  boolean existsByUsernameIgnoreCase(@Param("username") String username);

  @Query("SELECT a FROM Account a WHERE LOWER(a.username) = LOWER(:username)")
  Optional<Account> findByUsernameIgnoreCase(@Param("username") String username);

  Optional<Account> findByIdAndOrganizationId(Long id, Long organizationId);

  @Query(
      """
      SELECT a FROM Account a
      WHERE a.organizationId = :organizationId
        AND (:institutionId IS NULL OR a.institutionId = :institutionId)
        AND (:role IS NULL OR a.role = :role)
        AND (:active IS NULL OR a.active = :active)
      """)
  Page<Account> search(
      @Param("organizationId") Long organizationId,
      @Param("institutionId") Long institutionId,
      @Param("role") AccountRole role,
      @Param("active") Boolean active,
      Pageable pageable);
}
