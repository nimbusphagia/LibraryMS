package com.nimbusphagia.library.inventory.internal.dto.stock;

import com.nimbusphagia.library.inventory.internal.entity.MovementReason;
import java.time.Instant;
import java.util.List;

public record StockEntryResponse(
    Long movementId,
    Long libraryId,
    MovementReason reason,
    String notes,
    Instant createdAt,
    List<StockEntryCopyResponse> copies) {}
