package com.nimbusphagia.library_ms.catalog.internal.dto.deweyTag;

import com.nimbusphagia.library_ms.catalog.internal.entity.DeweyTag;

public record DeweyTagRequest(String name, String description) {

  public DeweyTagRequest {
    name = (name == null || name.isBlank()) ? null : name.strip();
    description = (description == null || description.isBlank()) ? null : description.strip();
  }

  public DeweyTag replace(DeweyTag tag) {
    tag.setName(name);
    tag.setDescription(description);
    return tag;
  }
}
