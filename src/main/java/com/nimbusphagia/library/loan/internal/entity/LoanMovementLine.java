package com.nimbusphagia.library.loan.internal.entity;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.shared.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loan_movement_line")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanMovementLine extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "loan_movement_id", nullable = false)
  private LoanMovement movement;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "copy_id", nullable = false)
  private Copy copy;

  private Instant resolvedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resolved_by_id")
  private Account resolvedBy;

  @Enumerated(EnumType.STRING)
  private LoanOutcome outcome;
}
