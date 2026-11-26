package com.nimbusphagia.library.inventory.internal.dto.stock;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record StockTransferRequest(
    @NotNull(message = "Destination library is missing") Long destinationLibraryId,
    String notes,
    @NotEmpty(message = "At least one copy is required")
        @Size(max = 100, message = "A single transfer can have at most 100 copies")
        List<@NotNull(message = "Copy id is missing") Long> copyIds) {

  public StockTransferRequest {
    notes = notes == null || notes.isBlank() ? null : notes.strip();
  }
}
