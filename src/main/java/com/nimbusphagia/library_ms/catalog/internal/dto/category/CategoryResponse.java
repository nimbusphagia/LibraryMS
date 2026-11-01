package com.nimbusphagia.library_ms.catalog.internal.dto.category;

import com.nimbusphagia.library_ms.catalog.internal.entity.Category;

public record CategoryResponse(Long id, String name, String code) {
  public static CategoryResponse fromEntity(Category category) {
    return new CategoryResponse(category.getId(), category.getName(), category.getCode());
  }
}
