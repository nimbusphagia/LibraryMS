package com.nimbusphagia.library.catalog.internal.service;

import com.nimbusphagia.library.catalog.internal.dto.category.CategoryRequest;
import com.nimbusphagia.library.catalog.internal.dto.category.CategoryResponse;
import com.nimbusphagia.library.catalog.internal.entity.Category;
import com.nimbusphagia.library.catalog.internal.repository.CategoryRepository;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {
  private final CategoryRepository categoryRepository;

  @Transactional
  public CategoryResponse create(CategoryRequest request) {
    if (categoryRepository.existsByNameIgnoreCase(request.name())) {
      throw new ConflictException("Category name already exists");
    }
    if (categoryRepository.existsByCodeIgnoreCase(request.code())) {
      throw new ConflictException("Category code already exists");
    }
    Category category = categoryRepository.save(request.toEntity());
    return CategoryResponse.fromEntity(category);
  }

  @Transactional
  public CategoryResponse edit(Long id, CategoryRequest request) {
    Category category =
        categoryRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
      throw new ConflictException("Category name already exists");
    }
    if (categoryRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
      throw new ConflictException("Category code already exists");
    }
    request.replace(category);
    return CategoryResponse.fromEntity(category);
  }

  @Transactional(readOnly = true)
  public Page<CategoryResponse> findAll(String search, Pageable pageable) {
    Page<Category> page =
        (search == null || search.isBlank())
            ? categoryRepository.findAll(pageable)
            : categoryRepository.search(search.trim(), pageable);
    return page.map(CategoryResponse::fromEntity);
  }

  @Transactional
  public void delete(Long id) {
    Category category =
        categoryRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    if (categoryRepository.isReferenced(id)) {
      throw new ConflictException("Category has books and cannot be deleted");
    }
    categoryRepository.delete(category);
  }
}
