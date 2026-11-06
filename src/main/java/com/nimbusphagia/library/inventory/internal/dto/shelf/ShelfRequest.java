package com.nimbusphagia.library.inventory.internal.dto.shelf;

import com.nimbusphagia.library.inventory.internal.entity.Library;
import com.nimbusphagia.library.inventory.internal.entity.Shelf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShelfRequest(
    @NotBlank(message = "Shelf name is missing") @Size(max = 255) String name,
    @Size(max = 255) String description) {

  public ShelfRequest {
    name = name == null ? null : name.strip();
    description = description == null || description.isBlank() ? null : description.strip();
  }

  public Shelf toEntity(Library library) {
    return Shelf.builder().library(library).name(name).description(description).build();
  }

  public Shelf replace(Shelf shelf) {
    shelf.setName(name);
    shelf.setDescription(description);
    return shelf;
  }
}
