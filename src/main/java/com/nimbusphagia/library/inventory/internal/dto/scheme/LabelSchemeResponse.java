package com.nimbusphagia.library.inventory.internal.dto.scheme;

import com.nimbusphagia.library.inventory.internal.entity.LabelLayout;
import com.nimbusphagia.library.inventory.internal.entity.LabelScheme;

public record LabelSchemeResponse(Long id, String name, boolean template, LabelLayout layout) {
  public static LabelSchemeResponse fromEntity(LabelScheme scheme) {
    return new LabelSchemeResponse(
        scheme.getId(), scheme.getName(), scheme.isTemplate(), scheme.getLayout());
  }
}
