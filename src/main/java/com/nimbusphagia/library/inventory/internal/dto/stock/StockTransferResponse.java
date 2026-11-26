package com.nimbusphagia.library.inventory.internal.dto.stock;

import java.util.List;

public record StockTransferResponse(
    Long exitMovementId,
    Long entryMovementId,
    Long destinationLibraryId,
    List<TransferredCopy> copies) {

  public record TransferredCopy(Long fromCopyId, Long copyId, Long bookId, int copyNumber) {}
}
