package com.nimbusphagia.library.inventory.internal.dto.label;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// Each copy with the text printed for it, so a pattern that changed since is noticed
public record MarkPlacedRequest(
    @NotEmpty(message = "At least one label is required") List<@Valid @NotNull Placed> labels) {

  public record Placed(
      @NotNull(message = "Copy id is missing") Long copyId,
      @NotBlank(message = "The printed text is missing") String text) {}
}
