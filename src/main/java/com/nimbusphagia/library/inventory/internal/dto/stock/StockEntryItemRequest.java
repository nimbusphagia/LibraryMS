package com.nimbusphagia.library.inventory.internal.dto.stock;

import com.nimbusphagia.library.inventory.internal.dto.label.LabelParts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockEntryItemRequest(
    @NotNull(message = "Book id is missing") Long bookId,
    @NotNull(message = "Quantity is missing")
        @Min(value = 1, message = "Quantity must be between 1 and 100")
        @Max(value = 100, message = "Quantity must be between 1 and 100")
        Integer quantity,
    Long shelfId,
    @Valid LabelParts labelParts) {}
