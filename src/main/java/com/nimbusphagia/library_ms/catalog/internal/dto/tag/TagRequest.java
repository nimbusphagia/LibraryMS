package com.nimbusphagia.library_ms.catalog.internal.dto.tag;

import com.nimbusphagia.library_ms.catalog.internal.entity.Tag;
import jakarta.validation.constraints.NotBlank;

public record TagRequest(@NotBlank(message = "Tag name is missing") String name) {

  public TagRequest {
    name = name == null ? null : name.strip();
  }

  public Tag toEntity() {
    return Tag.builder().name(name).build();
  }

  public Tag replace(Tag tag) {
    tag.setName(name);
    return tag;
  }
}
