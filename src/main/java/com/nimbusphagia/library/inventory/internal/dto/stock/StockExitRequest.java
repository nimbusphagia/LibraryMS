package com.nimbusphagia.library.inventory.internal.dto.stock;

import com.nimbusphagia.library.inventory.internal.entity.MovementReason;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record StockExitRequest(
    @NotNull(message = "Reason is missing") MovementReason reason,
    String notes,
    @NotEmpty(message = "At least one copy is required")
        @Size(max = 100, message = "A single exit can have at most 100 copies")
        List<@NotNull(message = "Copy id is missing") Long> copyIds) {

  public StockExitRequest {
    notes = notes == null || notes.isBlank() ? null : notes.strip();
  }
}
