package com.nimbusphagia.library.catalog.internal.dto.category;

import com.nimbusphagia.library.catalog.internal.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank(message = "Category name is missing") @Size(max = 255) String name,
    @NotBlank(message = "Category code is missing") @Size(max = 255) String code) {

  public CategoryRequest {
    name = name == null ? null : name.strip();
    code = code == null ? null : code.strip();
  }

  public Category toEntity() {
    return Category.builder().name(name).code(code).build();
  }

  public Category replace(Category category) {
    category.setName(name);
    category.setCode(code);
    return category;
  }
}
