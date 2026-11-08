package com.nimbusphagia.library.loan.internal.dto.loan;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record LoanRequest(
    @NotNull(message = "Visitor is missing") Long visitorId,
    @NotEmpty(message = "At least one copy is required")
        @Size(max = 100, message = "A single loan can have at most 100 copies")
        List<@NotNull(message = "Copy id is missing") Long> copyIds,
    String notes) {

  public LoanRequest {
    notes = notes == null || notes.isBlank() ? null : notes.strip();
  }
}
