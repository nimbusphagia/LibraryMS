package com.nimbusphagia.library.catalog.internal.dto.tag;

import com.nimbusphagia.library.catalog.internal.entity.Tag;

public record TagResponse(Long id, Long institutionId, String name) {
  public static TagResponse fromEntity(Tag tag) {
    return new TagResponse(tag.getId(), tag.getInstitutionId(), tag.getName());
  }
}
