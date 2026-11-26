package com.nimbusphagia.library.catalog.internal.dto.publisher;

import com.nimbusphagia.library.catalog.internal.entity.Publisher;

public record PublisherResponse(Long id, Long institutionId, String name) {
  public static PublisherResponse fromEntity(Publisher publisher) {
    return new PublisherResponse(
        publisher.getId(), publisher.getInstitutionId(), publisher.getName());
  }
}
