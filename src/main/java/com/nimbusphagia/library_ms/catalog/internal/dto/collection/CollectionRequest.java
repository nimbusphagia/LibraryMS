package com.nimbusphagia.library_ms.catalog.internal.dto.collection;

import com.nimbusphagia.library_ms.catalog.internal.entity.Collection;
import com.nimbusphagia.library_ms.catalog.internal.entity.Publisher;
import jakarta.validation.constraints.NotBlank;

public record CollectionRequest(@NotBlank(message = "Collection title is missing") String name) {
  public CollectionRequest {
    name = name == null ? null : name.strip();
  }

  public Collection toEntity(Publisher publisher) {
    return Collection.builder().name(name).publisher(publisher).build();
  }

  public Collection replace(Collection collection) {
    collection.setName(name);
    return collection;
  }
}
