package com.nimbusphagia.library.inventory.internal.dto.label;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LabelCustomizeRequest(
    @Size(max = 255) String categoryCode,
    @Size(max = 255) String titleCode,
    @Size(max = 255) String deweyCode,
    @Size(max = 255) String malagaCode,
    @Positive Integer collectionVolume,
    @Positive Integer seriesVolume,
    @NotBlank(message = "A change reason is required") String changeReason) {

  public LabelCustomizeRequest {
    changeReason = changeReason == null ? null : changeReason.strip();
  }

  public LabelParts toParts() {
    return new LabelParts(
        categoryCode, titleCode, deweyCode, malagaCode, collectionVolume, seriesVolume);
  }
}
