package com.nimbusphagia.library_ms.inventory.internal.entity;

import com.nimbusphagia.library_ms.account.internal.entity.Account;
import com.nimbusphagia.library_ms.shared.model.ReadOnlyEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock-movement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement extends ReadOnlyEntity {

  Long quantity;

  @Enumerated(EnumType.STRING)
  MovementType type;

  @ManyToOne
  @JoinColumn(name = "library_id")
  Library library;

  @ManyToOne
  @JoinColumn(name = "account_id")
  Account signedBy;
}
