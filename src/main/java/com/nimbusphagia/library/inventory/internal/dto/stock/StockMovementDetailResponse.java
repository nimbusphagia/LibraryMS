package com.nimbusphagia.library.inventory.internal.dto.stock;

import com.nimbusphagia.library.inventory.internal.entity.MovementReason;
import com.nimbusphagia.library.inventory.internal.entity.MovementType;
import java.time.Instant;
import java.util.List;

public record StockMovementDetailResponse(
    Long id,
    Long libraryId,
    MovementType type,
    MovementReason reason,
    String notes,
    Long signedById,
    String signedByName,
    Instant createdAt,
    List<StockMovementLineResponse> copies) {}
