package com.nimbusphagia.library.inventory.internal.dto.stock;

import com.nimbusphagia.library.inventory.internal.entity.MovementReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record StockEntryRequest(
    @NotNull(message = "Reason is missing") MovementReason reason,
    String notes,
    @NotEmpty(message = "At least one item is required")
        @Size(max = 50, message = "A single entry can have at most 50 items")
        @Valid
        List<StockEntryItemRequest> items) {

  public StockEntryRequest {
    notes = notes == null || notes.isBlank() ? null : notes.strip();
  }
}
