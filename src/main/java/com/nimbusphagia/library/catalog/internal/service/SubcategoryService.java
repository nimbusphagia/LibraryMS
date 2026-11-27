package com.nimbusphagia.library.catalog.internal.service;

import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.anyContains;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.attributeIs;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.idNot;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.textEquals;

import com.nimbusphagia.library.catalog.LabelledBooks;
import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.dto.subcategory.SubcategoryRequest;
import com.nimbusphagia.library.catalog.internal.dto.subcategory.SubcategoryResponse;
import com.nimbusphagia.library.catalog.internal.entity.Category;
import com.nimbusphagia.library.catalog.internal.entity.Subcategory;
import com.nimbusphagia.library.catalog.internal.repository.BookRepository;
import com.nimbusphagia.library.catalog.internal.repository.CategoryRepository;
import com.nimbusphagia.library.catalog.internal.repository.SubcategoryRepository;
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
public class SubcategoryService {
  private final SubcategoryRepository subcategoryRepository;
  private final CategoryRepository categoryRepository;
  private final BookRepository bookRepository;
  private final LabelledBooks labelledBooks;
  private final CatalogAccess access;
  private final LabelDataEvents labelDataEvents;

  @Transactional
  public SubcategoryResponse create(
      Long institutionId, Long categoryId, SubcategoryRequest request) {
    Category category = findCategory(institutionId, categoryId);
    Subcategory subcategory = access.stamp(request.toEntity(category), institutionId);
    requireUnique(institutionId, categoryId, request, null);
    return SubcategoryResponse.fromEntity(subcategoryRepository.save(subcategory));
  }

  @Transactional
  public SubcategoryResponse edit(
      Long institutionId, Long categoryId, Long subcategoryId, SubcategoryRequest request) {
    Subcategory subcategory = findOwned(institutionId, categoryId, subcategoryId);
    requireUnique(institutionId, categoryId, request, subcategoryId);
    boolean codeChanges = !subcategory.getCode().equals(request.code());
    // The code is printed on labels; only admins may leave labels outdated
    if (codeChanges
        && !access.isAdmin()
        && labelledBooks.anyLabelled(bookRepository.findIdsBySubcategoryId(subcategoryId))) {
      throw new ConflictException(
          ErrorCodes.LABEL_DATA_LOCKED,
          "Books of this subcategory have labels; ask an admin to change its code");
    }
    request.replace(subcategory);
    if (codeChanges) {
      labelDataEvents.changed(bookRepository.findIdsBySubcategoryId(subcategoryId));
    }
    return SubcategoryResponse.fromEntity(subcategory);
  }

  @Transactional(readOnly = true)
  public Page<SubcategoryResponse> findAll(
      Long institutionId, Long categoryId, CatalogScope scope, String search, Pageable pageable) {
    findCategory(institutionId, categoryId);
    return subcategoryRepository
        .findAll(
            access
                .<Subcategory>in(institutionId, scope)
                .and(attributeIs("category", categoryId))
                .and(anyContains(search, "name", "code")),
            pageable)
        .map(SubcategoryResponse::fromEntity);
  }

  @Transactional
  public void delete(Long institutionId, Long categoryId, Long subcategoryId) {
    Subcategory subcategory = findOwned(institutionId, categoryId, subcategoryId);
    if (subcategoryRepository.isReferenced(subcategoryId)) {
      throw new ConflictException("Subcategory has books and cannot be deleted");
    }
    subcategoryRepository.delete(subcategory);
  }

  // Helper
  private Category findCategory(Long institutionId, Long categoryId) {
    return access
        .visible(categoryRepository.findById(categoryId), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
  }

  private Subcategory findOwned(Long institutionId, Long categoryId, Long subcategoryId) {
    access.requireWriter(institutionId);
    return access
        .owned(
            subcategoryRepository.findByIdAndCategoryId(subcategoryId, categoryId), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found"));
  }

  private void requireUnique(
      Long institutionId, Long categoryId, SubcategoryRequest request, Long id) {
    if (subcategoryRepository.exists(
        access
            .<Subcategory>visible(institutionId)
            .and(attributeIs("category", categoryId))
            .and(textEquals("name", request.name()))
            .and(idNot(id)))) {
      throw new ConflictException("Subcategory name already exists in this category");
    }
    if (subcategoryRepository.exists(
        access
            .<Subcategory>visible(institutionId)
            .and(attributeIs("category", categoryId))
            .and(textEquals("code", request.code()))
            .and(idNot(id)))) {
      throw new ConflictException("Subcategory code already exists in this category");
    }
  }
}
