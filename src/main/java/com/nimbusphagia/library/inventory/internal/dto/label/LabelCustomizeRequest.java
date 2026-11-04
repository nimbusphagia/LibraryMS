package com.nimbusphagia.library.inventory.internal.dto.label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabelCustomizeRequest(
    @NotBlank(message = "Label text is missing") @Size(max = 255) String text,
    @NotBlank(message = "A change reason is required") String changeReason) {

  public LabelCustomizeRequest {
    text = text == null ? null : text.strip();
    changeReason = changeReason == null ? null : changeReason.strip();
  }
}
