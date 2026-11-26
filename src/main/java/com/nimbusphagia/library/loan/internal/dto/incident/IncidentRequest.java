package com.nimbusphagia.library.loan.internal.dto.incident;

import jakarta.validation.constraints.NotNull;

public record IncidentRequest(
    @NotNull(message = "Incident type is missing") Long typeId, Long loanLineId, String notes) {

  public IncidentRequest {
    notes = notes == null || notes.isBlank() ? null : notes.strip();
  }
}
