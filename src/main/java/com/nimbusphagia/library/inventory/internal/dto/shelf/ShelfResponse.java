package com.nimbusphagia.library.inventory.internal.dto.shelf;

import com.nimbusphagia.library.inventory.internal.entity.Shelf;

public record ShelfResponse(
    Long id, Long libraryId, String name, String description, boolean defaultShelf) {

  public static ShelfResponse fromEntity(Shelf shelf) {
    return new ShelfResponse(
        shelf.getId(),
        shelf.getLibrary().getId(),
        shelf.getName(),
        shelf.getDescription(),
        shelf.isDefaultShelf());
  }
}
