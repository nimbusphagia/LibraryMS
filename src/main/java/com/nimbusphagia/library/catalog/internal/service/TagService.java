package com.nimbusphagia.library.catalog.internal.service;

import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.anyContains;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.idNot;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.textEquals;

import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.dto.tag.TagRequest;
import com.nimbusphagia.library.catalog.internal.dto.tag.TagResponse;
import com.nimbusphagia.library.catalog.internal.entity.Tag;
import com.nimbusphagia.library.catalog.internal.repository.TagRepository;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {
  private final TagRepository tagRepository;
  private final CatalogAccess access;

  @Transactional
  public TagResponse create(Long institutionId, TagRequest request) {
    Tag tag = access.stamp(request.toEntity(), institutionId);
    requireUniqueName(institutionId, request.name(), null);
    return TagResponse.fromEntity(tagRepository.save(tag));
  }

  @Transactional
  public TagResponse edit(Long institutionId, Long id, TagRequest request) {
    Tag tag = findOwned(institutionId, id);
    requireUniqueName(institutionId, request.name(), id);
    request.replace(tag);
    return TagResponse.fromEntity(tag);
  }

  @Transactional(readOnly = true)
  public Page<TagResponse> findAll(
      Long institutionId, CatalogScope scope, String search, Pageable pageable) {
    return tagRepository
        .findAll(access.<Tag>in(institutionId, scope).and(anyContains(search, "name")), pageable)
        .map(TagResponse::fromEntity);
  }

  @Transactional
  public void delete(Long institutionId, Long id) {
    Tag tag = findOwned(institutionId, id);
    if (tagRepository.isReferenced(id)) {
      throw new ConflictException("Tag has books and cannot be deleted");
    }
    tagRepository.delete(tag);
  }

  // Helper
  private Tag findOwned(Long institutionId, Long id) {
    access.requireWriter(institutionId);
    return access
        .owned(tagRepository.findById(id), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
  }

  private void requireUniqueName(Long institutionId, String name, Long id) {
    if (tagRepository.exists(
        access.<Tag>visible(institutionId).and(textEquals("name", name)).and(idNot(id)))) {
      throw new ConflictException("Tag name already exists");
    }
  }
}
