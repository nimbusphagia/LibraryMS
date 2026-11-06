package com.nimbusphagia.library.inventory.internal.dto.stock;

import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;

public record StockMovementLineResponse(
    Long copyId,
    Long bookId,
    String bookTitle,
    int copyNumber,
    Long shelfId,
    String shelfName,
    CopyStatus currentStatus) {}
