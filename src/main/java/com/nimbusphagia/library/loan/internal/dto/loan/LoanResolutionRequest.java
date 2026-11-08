package com.nimbusphagia.library.loan.internal.dto.loan;

import com.nimbusphagia.library.loan.internal.entity.LoanOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record LoanResolutionRequest(
    @NotEmpty(message = "At least one line is required")
        @Size(max = 100, message = "A single resolution can have at most 100 lines")
        List<@NotNull(message = "Line is missing") @Valid LineResolution> lines,
    String notes) {

  public LoanResolutionRequest {
    notes = notes == null || notes.isBlank() ? null : notes.strip();
  }

  public record LineResolution(
      @NotNull(message = "Line id is missing") Long lineId,
      @NotNull(message = "Outcome is missing") LoanOutcome outcome) {}
}
