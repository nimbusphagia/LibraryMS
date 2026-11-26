package com.nimbusphagia.library.inventory.internal.entity;

import com.nimbusphagia.library.shared.model.ReadOnlyEntity;
import jakarta.persistence.Entity;
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
@Table(name = "stock_movement_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementLine extends ReadOnlyEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "movement_id", nullable = false)
  private StockMovement movement;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "copy_id", nullable = false)
  private Copy copy;

  // Loan line whose outcome caused this exit; an id because loan depends on inventory
  private Long loanLineId;

  // The LOSS exit line a RECOVERY entry undoes
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recovered_line_id")
  private StockMovementLine recoveredLine;
}
