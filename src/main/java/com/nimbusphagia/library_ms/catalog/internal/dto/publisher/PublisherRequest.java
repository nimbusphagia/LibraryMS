package com.nimbusphagia.library_ms.catalog.internal.dto.publisher;

import com.nimbusphagia.library_ms.catalog.internal.entity.Publisher;
import jakarta.validation.constraints.NotBlank;

public record PublisherRequest(@NotBlank(message = "Publisher name is missing") String name) {

  public PublisherRequest {
    name = name == null ? null : name.strip();
  }

  public Publisher toEntity() {
    return Publisher.builder().name(name).build();
  }

  public Publisher replace(Publisher publisher) {
    publisher.setName(name);
    return publisher;
  }
}
