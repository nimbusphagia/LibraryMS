package com.nimbusphagia.library_ms.account.internal.entity;

import com.nimbusphagia.library_ms.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseEntity {
  @Column(nullable = false)
  private String username;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private String fullName;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountRole role = AccountRole.STAFF;

  @Builder.Default
  @Column(nullable = false)
  private boolean active = true;
}
