package com.nimbusphagia.library.catalog.internal.dto.collection;

import com.nimbusphagia.library.catalog.internal.entity.Collection;

public record CollectionResponse(Long id, Long institutionId, String name, String code) {

  public static CollectionResponse fromEntity(Collection collection) {
    return new CollectionResponse(
        collection.getId(),
        collection.getInstitutionId(),
        collection.getName(),
        collection.getCode());
  }
}
