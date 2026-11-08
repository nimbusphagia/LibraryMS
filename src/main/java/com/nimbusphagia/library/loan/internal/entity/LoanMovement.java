package com.nimbusphagia.library.loan.internal.entity;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.inventory.internal.entity.Library;
import com.nimbusphagia.library.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "loan_movement")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanMovement extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "library_id", nullable = false)
  private Library library;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "loaned_by_id", nullable = false)
  private Account loanedBy;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "loaned_to_id", nullable = false)
  private Visitor loanedTo;

  @Column(nullable = false)
  private Instant expiresBy;

  @Column(columnDefinition = "text")
  private String notes;
}
