package com.nimbusphagia.library.inventory.internal.dto.stock;

public record StockEntryCopyResponse(
    Long copyId,
    Long bookId,
    int copyNumber,
    Long shelfId,
    String shelfName,
    Long labelId,
    String labelText) {}
