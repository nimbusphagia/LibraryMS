package com.nimbusphagia.library.catalog.internal.dto.collection;

import com.nimbusphagia.library.catalog.internal.entity.Collection;

public record CollectionResponse(Long id, String name, String code) {

  public static CollectionResponse fromEntity(Collection collection) {
    return new CollectionResponse(collection.getId(), collection.getName(), collection.getCode());
  }
}
