package com.nimbusphagia.library_ms.inventory.internal.entity;

import com.nimbusphagia.library_ms.account.internal.entity.Account;
import com.nimbusphagia.library_ms.shared.model.ReadOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_movement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement extends ReadOnlyEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "library_id", nullable = false)
  private Library library;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MovementType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MovementReason reason;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "signed_by_id", nullable = false)
  private Account signedBy;

  @Column(columnDefinition = "text")
  private String notes;
}
