package com.nimbusphagia.library.inventory.internal.dto.library;

import com.nimbusphagia.library.inventory.internal.entity.Library;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LibraryRequest(
    @NotBlank(message = "Library name is missing") @Size(max = 255) String name) {

  public LibraryRequest {
    name = name == null ? null : name.strip();
  }

  public Library toEntity() {
    return Library.builder().name(name).build();
  }

  public Library replace(Library library) {
    library.setName(name);
    return library;
  }
}
