package com.nimbusphagia.library.inventory.internal.dto.label;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LabelParts(
    @Size(max = 255) String categoryCode,
    @Size(max = 255) String titleCode,
    @Size(max = 255) String deweyCode,
    @Size(max = 255) String malagaCode,
    @Positive Integer collectionVolume,
    @Positive Integer seriesVolume) {

  public LabelParts {
    categoryCode = clean(categoryCode);
    titleCode = clean(titleCode);
    deweyCode = clean(deweyCode);
    malagaCode = clean(malagaCode);
  }

  public boolean isEmpty() {
    return categoryCode == null
        && titleCode == null
        && deweyCode == null
        && malagaCode == null
        && collectionVolume == null
        && seriesVolume == null;
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }
}
