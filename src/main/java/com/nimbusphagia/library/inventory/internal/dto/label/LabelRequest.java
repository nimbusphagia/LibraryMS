package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.catalog.LabelPart;
import jakarta.validation.constraints.Size;
import java.util.Map;

// Parts left out take their derived value
public record LabelRequest(Map<LabelPart, @Size(max = 50) String> parts) {
  public LabelRequest {
    parts = parts == null ? Map.of() : parts;
  }
}
