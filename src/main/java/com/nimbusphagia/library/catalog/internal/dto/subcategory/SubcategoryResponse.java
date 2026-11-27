package com.nimbusphagia.library.catalog.internal.dto.subcategory;

import com.nimbusphagia.library.catalog.internal.entity.Subcategory;

public record SubcategoryResponse(Long id, Long institutionId, String name, String code) {
  public static SubcategoryResponse fromEntity(Subcategory subcategory) {
    return new SubcategoryResponse(
        subcategory.getId(),
        subcategory.getInstitutionId(),
        subcategory.getName(),
        subcategory.getCode());
  }
}
