package com.nimbusphagia.library.inventory.internal.dto.library;

import com.nimbusphagia.library.inventory.internal.entity.Library;

public record LibraryResponse(Long id, String name, boolean active) {
  public static LibraryResponse fromEntity(Library library) {
    return new LibraryResponse(library.getId(), library.getName(), library.isActive());
  }
}
