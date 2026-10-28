package com.nimbusphagia.library_ms.catalog.internal.service;

import com.nimbusphagia.library_ms.catalog.internal.dto.tag.TagRequest;
import com.nimbusphagia.library_ms.catalog.internal.dto.tag.TagResponse;
import com.nimbusphagia.library_ms.catalog.internal.entity.Tag;
import com.nimbusphagia.library_ms.catalog.internal.repository.TagRepository;
import com.nimbusphagia.library_ms.exception.custom.ConflictException;
import com.nimbusphagia.library_ms.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {
  private final TagRepository tagRepository;

  @Transactional
  public TagResponse create(TagRequest request) {
    if (tagRepository.existsByNameIgnoreCase(request.name())) {
      throw new ConflictException("Tag name already exists");
    }
    Tag tag = tagRepository.save(request.toEntity());
    return TagResponse.fromEntity(tag);
  }

  @Transactional
  public TagResponse edit(Long id, TagRequest request) {
    Tag tag =
        tagRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
    if (tagRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
      throw new ConflictException("Tag name already exists");
    }
    request.replace(tag);
    return TagResponse.fromEntity(tag);
  }

  @Transactional(readOnly = true)
  public Page<TagResponse> findAll(String search, Pageable pageable) {
    Page<Tag> page =
        (search == null || search.isBlank())
            ? tagRepository.findAll(pageable)
            : tagRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
    return page.map(TagResponse::fromEntity);
  }

  @Transactional
  public void delete(Long id) {
    Tag tag =
        tagRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
    if (tagRepository.isReferenced(id)) {
      throw new ConflictException("Tag has books and cannot be deleted");
    }
    tagRepository.delete(tag);
  }
}
