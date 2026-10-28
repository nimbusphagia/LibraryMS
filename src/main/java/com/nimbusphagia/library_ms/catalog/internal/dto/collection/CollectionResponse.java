package com.nimbusphagia.library_ms.catalog.internal.dto.collection;

import com.nimbusphagia.library_ms.catalog.internal.entity.Collection;

public record CollectionResponse(Long id, String name) {

  public static CollectionResponse fromEntity(Collection collection) {
    return new CollectionResponse(collection.getId(), collection.getName());
  }
}
