package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.catalog.LabelPart;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record LabelCustomizeRequest(
    Map<LabelPart, @Size(max = 50) String> parts,
    @NotBlank(message = "A change reason is required") String changeReason) {

  public LabelCustomizeRequest {
    parts = parts == null ? Map.of() : parts;
    changeReason = changeReason == null ? null : changeReason.strip();
  }
}
