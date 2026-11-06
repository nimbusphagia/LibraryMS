package com.nimbusphagia.library.inventory.internal.dto.stock;

import com.nimbusphagia.library.inventory.internal.entity.MovementReason;
import com.nimbusphagia.library.inventory.internal.entity.MovementType;
import com.nimbusphagia.library.inventory.internal.entity.StockMovement;
import java.time.Instant;

public record StockMovementSummaryResponse(
    Long id,
    Long libraryId,
    MovementType type,
    MovementReason reason,
    String notes,
    Long signedById,
    String signedByName,
    Instant createdAt,
    long copyCount) {

  public static StockMovementSummaryResponse fromEntity(StockMovement m, long copyCount) {
    return new StockMovementSummaryResponse(
        m.getId(),
        m.getLibrary().getId(),
        m.getType(),
        m.getReason(),
        m.getNotes(),
        m.getSignedBy().getId(),
        m.getSignedBy().getFullName(),
        m.getCreatedAt(),
        copyCount);
  }
}
