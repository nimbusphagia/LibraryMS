package com.nimbusphagia.library.inventory.internal.dto.copy;

import jakarta.validation.constraints.NotBlank;

public record CopyConditionRequest(@NotBlank(message = "Reason is missing") String reason) {

  public CopyConditionRequest {
    reason = reason == null ? null : reason.strip();
  }
}
