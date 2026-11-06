package com.nimbusphagia.library.inventory.internal.dto.stock;

import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;

public record StockExitCopyResponse(Long copyId, Long bookId, int copyNumber, CopyStatus status) {}
