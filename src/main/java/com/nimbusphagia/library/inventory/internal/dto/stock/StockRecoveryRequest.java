package com.nimbusphagia.library.inventory.internal.dto.stock;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record StockRecoveryRequest(
    String notes,
    @NotEmpty(message = "At least one copy is required")
        @Size(max = 100, message = "A single recovery can have at most 100 copies")
        List<@NotNull(message = "Copy id is missing") Long> copyIds) {

  public StockRecoveryRequest {
    notes = notes == null || notes.isBlank() ? null : notes.strip();
  }
}
