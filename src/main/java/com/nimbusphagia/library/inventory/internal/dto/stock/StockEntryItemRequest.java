package com.nimbusphagia.library.inventory.internal.dto.stock;

import com.nimbusphagia.library.catalog.LabelPart;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record StockEntryItemRequest(
    @NotNull(message = "Book id is missing") Long bookId,
    @NotNull(message = "Quantity is missing")
        @Min(value = 1, message = "Quantity must be between 1 and 100")
        @Max(value = 100, message = "Quantity must be between 1 and 100")
        Integer quantity,
    Long shelfId,
    // Null registers the copies without labels; parts left out take their derived value
    Map<LabelPart, @Size(max = 50) String> labelParts) {}
