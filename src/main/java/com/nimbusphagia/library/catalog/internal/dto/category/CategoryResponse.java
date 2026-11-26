package com.nimbusphagia.library.catalog.internal.dto.category;

import com.nimbusphagia.library.catalog.internal.entity.Category;

public record CategoryResponse(Long id, Long institutionId, String name, String code) {
  public static CategoryResponse fromEntity(Category category) {
    return new CategoryResponse(
        category.getId(), category.getInstitutionId(), category.getName(), category.getCode());
  }
}
