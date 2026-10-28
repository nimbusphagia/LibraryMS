package com.nimbusphagia.library_ms.catalog.internal.dto.tag;

import com.nimbusphagia.library_ms.catalog.internal.entity.Tag;

public record TagResponse(Long id, String name) {
  public static TagResponse fromEntity(Tag tag) {
    return new TagResponse(tag.getId(), tag.getName());
  }
}
