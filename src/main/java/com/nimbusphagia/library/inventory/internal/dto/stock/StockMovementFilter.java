package com.nimbusphagia.library.inventory.internal.dto.stock;

import com.nimbusphagia.library.inventory.internal.entity.MovementReason;
import com.nimbusphagia.library.inventory.internal.entity.MovementType;

public record StockMovementFilter(MovementType type, MovementReason reason) {}
