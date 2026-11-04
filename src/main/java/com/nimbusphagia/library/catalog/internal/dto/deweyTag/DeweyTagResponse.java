package com.nimbusphagia.library.catalog.internal.dto.deweyTag;

import com.nimbusphagia.library.catalog.internal.entity.DeweyTag;

public record DeweyTagResponse(Long id, String code, String name, String description) {

  public static DeweyTagResponse fromEntity(DeweyTag tag) {
    return new DeweyTagResponse(tag.getId(), tag.getCode(), tag.getName(), tag.getDescription());
  }
}
