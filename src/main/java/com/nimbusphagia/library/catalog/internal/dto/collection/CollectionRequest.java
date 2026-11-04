package com.nimbusphagia.library.catalog.internal.dto.collection;

import com.nimbusphagia.library.catalog.internal.entity.Collection;
import com.nimbusphagia.library.catalog.internal.entity.Publisher;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CollectionRequest(
    @NotBlank(message = "Collection title is missing") String name, @Size(max = 255) String code) {

  public CollectionRequest {
    name = name == null ? null : name.strip();
    code = code == null || code.isBlank() ? null : code.strip();
  }

  public Collection toEntity(Publisher publisher) {
    return Collection.builder().name(name).code(code).publisher(publisher).build();
  }

  public Collection replace(Collection collection) {
    collection.setName(name);
    collection.setCode(code);
    return collection;
  }
}
