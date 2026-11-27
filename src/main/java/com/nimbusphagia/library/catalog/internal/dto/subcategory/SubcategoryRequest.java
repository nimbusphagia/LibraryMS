package com.nimbusphagia.library.catalog.internal.dto.subcategory;

import com.nimbusphagia.library.catalog.internal.entity.Category;
import com.nimbusphagia.library.catalog.internal.entity.Subcategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubcategoryRequest(
    @NotBlank(message = "Subcategory name is missing") @Size(max = 255) String name,
    @NotBlank(message = "Subcategory code is missing") @Size(max = 255) String code) {

  public SubcategoryRequest {
    name = name == null ? null : name.strip();
    code = code == null ? null : code.strip();
  }

  public Subcategory toEntity(Category category) {
    return Subcategory.builder().name(name).code(code).category(category).build();
  }

  public Subcategory replace(Subcategory subcategory) {
    subcategory.setName(name);
    subcategory.setCode(code);
    return subcategory;
  }
}
