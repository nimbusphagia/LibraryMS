package com.nimbusphagia.library_ms.catalog.internal.service;

import com.nimbusphagia.library_ms.catalog.internal.dto.deweyTag.DeweyTagRequest;
import com.nimbusphagia.library_ms.catalog.internal.dto.deweyTag.DeweyTagResponse;
import com.nimbusphagia.library_ms.catalog.internal.entity.DeweyTag;
import com.nimbusphagia.library_ms.catalog.internal.repository.DeweyTagRepository;
import com.nimbusphagia.library_ms.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeweyTagService {
  private final DeweyTagRepository deweyTagRepository;

  @Transactional(readOnly = true)
  public DeweyTagResponse findOne(Long id) {
    return DeweyTagResponse.fromEntity(findTag(id));
  }

  @Transactional(readOnly = true)
  public Page<DeweyTagResponse> findAll(String search, Pageable pageable) {
    Page<DeweyTag> page =
        (search == null || search.isBlank())
            ? deweyTagRepository.findAll(pageable)
            : deweyTagRepository.search(search.trim(), pageable);
    return page.map(DeweyTagResponse::fromEntity);
  }

  @Transactional
  public DeweyTagResponse edit(Long id, DeweyTagRequest request) {
    DeweyTag tag = findTag(id);
    request.replace(tag);
    return DeweyTagResponse.fromEntity(tag);
  }

  // Helper
  private DeweyTag findTag(Long id) {
    return deweyTagRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Dewey tag not found"));
  }
}
