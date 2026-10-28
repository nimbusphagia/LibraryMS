package com.nimbusphagia.library_ms.catalog.internal.service;

import com.nimbusphagia.library_ms.catalog.internal.dto.publisher.PublisherRequest;
import com.nimbusphagia.library_ms.catalog.internal.dto.publisher.PublisherResponse;
import com.nimbusphagia.library_ms.catalog.internal.entity.Publisher;
import com.nimbusphagia.library_ms.catalog.internal.repository.PublisherRepository;
import com.nimbusphagia.library_ms.exception.custom.ConflictException;
import com.nimbusphagia.library_ms.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublisherService {
  private final PublisherRepository publisherRepository;

  @Transactional
  public PublisherResponse create(PublisherRequest request) {
    if (publisherRepository.existsByNameIgnoreCase(request.name())) {
      throw new ConflictException("Publisher name already exists");
    }
    Publisher publisher = publisherRepository.save(request.toEntity());
    return PublisherResponse.fromEntity(publisher);
  }

  @Transactional
  public PublisherResponse edit(Long id, PublisherRequest request) {
    Publisher publisher =
        publisherRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Publisher not found"));
    if (publisherRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
      throw new ConflictException("Publisher name already exists");
    }
    request.replace(publisher);
    return PublisherResponse.fromEntity(publisher);
  }

  @Transactional(readOnly = true)
  public Page<PublisherResponse> findAll(String search, Pageable pageable) {
    Page<Publisher> page =
        (search == null || search.isBlank())
            ? publisherRepository.findAll(pageable)
            : publisherRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
    return page.map(PublisherResponse::fromEntity);
  }

  @Transactional
  public void delete(Long id) {
    Publisher publisher =
        publisherRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Publisher not found"));
    if (publisherRepository.isReferenced(id)) {
      throw new ConflictException("Publisher has books or collections and cannot be deleted");
    }
    publisherRepository.delete(publisher);
  }
}
