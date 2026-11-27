package com.nimbusphagia.library.inventory.internal.dto.scheme;

import com.nimbusphagia.library.inventory.internal.entity.LabelLayout;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LabelSchemeRequest(
    @NotBlank(message = "Scheme name is missing") @Size(max = 255) String name,
    @NotNull(message = "Layout is missing") @Valid LabelLayout layout) {

  public LabelSchemeRequest {
    name = name == null ? null : name.strip();
  }
}
