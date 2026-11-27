package com.nimbusphagia.library.inventory.internal.dto.library;

import com.nimbusphagia.library.inventory.internal.entity.LabelScheme;
import com.nimbusphagia.library.inventory.internal.entity.Library;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LibraryRequest(
    @NotBlank(message = "Library name is missing") @Size(max = 255) String name,
    // Read only on creation, where it defaults to the Spring template
    Long labelSchemeId) {

  public LibraryRequest {
    name = name == null ? null : name.strip();
  }

  public Library toEntity(Long institutionId, LabelScheme labelScheme) {
    return Library.builder()
        .institutionId(institutionId)
        .name(name)
        .labelScheme(labelScheme)
        .build();
  }

  public Library replace(Library library) {
    library.setName(name);
    return library;
  }
}
