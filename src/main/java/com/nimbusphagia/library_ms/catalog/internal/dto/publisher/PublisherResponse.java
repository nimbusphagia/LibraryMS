package com.nimbusphagia.library_ms.catalog.internal.dto.publisher;

import com.nimbusphagia.library_ms.catalog.internal.entity.Publisher;

public record PublisherResponse(Long id, String name) {
  public Publisher toEntity() {
    return Publisher.builder().name(name).build();
  }

  public static PublisherResponse fromEntity(Publisher publisher) {
    return new PublisherResponse(publisher.getId(), publisher.getName());
  }
}
