package com.nimbusphagia.library.inventory.internal.dto.stock;

// labelText is what the copy should show; null while its book has no label in the library
public record StockEntryCopyResponse(
    Long copyId,
    Long bookId,
    int copyNumber,
    Long shelfId,
    String shelfName,
    String labelText) {}
