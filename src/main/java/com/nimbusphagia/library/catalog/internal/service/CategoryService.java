package com.nimbusphagia.library.catalog.internal.service;

import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.anyContains;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.idNot;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.textEquals;

import com.nimbusphagia.library.catalog.LabelledBooks;
import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.dto.category.CategoryRequest;
import com.nimbusphagia.library.catalog.internal.dto.category.CategoryResponse;
import com.nimbusphagia.library.catalog.internal.entity.Category;
import com.nimbusphagia.library.catalog.internal.repository.BookRepository;
import com.nimbusphagia.library.catalog.internal.repository.CategoryRepository;
import com.nimbusphagia.library.exception.ErrorCodes;
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
  private final BookRepository bookRepository;
  private final LabelledBooks labelledBooks;
  private final CatalogAccess access;

  @Transactional
  public CategoryResponse create(Long institutionId, CategoryRequest request) {
    Category category = access.stamp(request.toEntity(), institutionId);
    requireUnique(institutionId, request, null);
    return CategoryResponse.fromEntity(categoryRepository.save(category));
  }

  @Transactional
  public CategoryResponse edit(Long institutionId, Long id, CategoryRequest request) {
    Category category = findOwned(institutionId, id);
    requireUnique(institutionId, request, id);
    // The code is printed on labels; only admins may leave labels outdated
    if (!category.getCode().equals(request.code())
        && !access.isAdmin()
        && labelledBooks.anyLabelled(bookRepository.findIdsByCategoryId(id))) {
      throw new ConflictException(
          ErrorCodes.LABEL_DATA_LOCKED,
          "Books of this category have labels; ask an admin to change its code");
    }
    request.replace(category);
    return CategoryResponse.fromEntity(category);
  }

  @Transactional(readOnly = true)
  public Page<CategoryResponse> findAll(
      Long institutionId, CatalogScope scope, String search, Pageable pageable) {
    return categoryRepository
        .findAll(
            access.<Category>in(institutionId, scope).and(anyContains(search, "name", "code")),
            pageable)
        .map(CategoryResponse::fromEntity);
  }

  @Transactional
  public void delete(Long institutionId, Long id) {
    Category category = findOwned(institutionId, id);
    if (categoryRepository.isReferenced(id)) {
      throw new ConflictException("Category has books or subcategories and cannot be deleted");
    }
    categoryRepository.delete(category);
  }

  // Helper
  private Category findOwned(Long institutionId, Long id) {
    access.requireWriter(institutionId);
    return access
        .owned(categoryRepository.findById(id), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
  }

  private void requireUnique(Long institutionId, CategoryRequest request, Long id) {
    if (categoryRepository.exists(
        access
            .<Category>visible(institutionId)
            .and(textEquals("name", request.name()))
            .and(idNot(id)))) {
      throw new ConflictException("Category name already exists");
    }
    if (categoryRepository.exists(
        access
            .<Category>visible(institutionId)
            .and(textEquals("code", request.code()))
            .and(idNot(id)))) {
      throw new ConflictException("Category code already exists");
    }
  }
}
