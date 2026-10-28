package com.nimbusphagia.library_ms.catalog.internal.service;

import com.nimbusphagia.library_ms.catalog.internal.dto.collection.CollectionRequest;
import com.nimbusphagia.library_ms.catalog.internal.dto.collection.CollectionResponse;
import com.nimbusphagia.library_ms.catalog.internal.entity.Collection;
import com.nimbusphagia.library_ms.catalog.internal.entity.Publisher;
import com.nimbusphagia.library_ms.catalog.internal.repository.CollectionRepository;
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
public class CollectionService {
  private final CollectionRepository collectionRepository;
  private final PublisherRepository publisherRepository;

  @Transactional
  public CollectionResponse create(Long publisherId, CollectionRequest request) {
    Publisher publisher = findPublisher(publisherId);
    if (collectionRepository.existsByPublisherAndName(publisherId, request.name())) {
      throw new ConflictException("Collection name already exists for this publisher");
    }
    Collection collection = collectionRepository.save(request.toEntity(publisher));
    return CollectionResponse.fromEntity(collection);
  }

  @Transactional
  public CollectionResponse edit(Long publisherId, Long collectionId, CollectionRequest request) {
    Collection collection = findCollection(publisherId, collectionId);
    if (collectionRepository.existsByPublisherAndNameAndIdNot(
        publisherId, request.name(), collectionId)) {
      throw new ConflictException("Collection name already exists for this publisher");
    }
    request.replace(collection);
    return CollectionResponse.fromEntity(collection);
  }

  @Transactional(readOnly = true)
  public Page<CollectionResponse> findAll(Long publisherId, String search, Pageable pageable) {
    if (!publisherRepository.existsById(publisherId)) {
      throw new ResourceNotFoundException("Publisher not found");
    }
    Page<Collection> page =
        (search == null || search.isBlank())
            ? collectionRepository.findByPublisherId(publisherId, pageable)
            : collectionRepository.findByPublisherIdAndNameContainingIgnoreCase(
                publisherId, search.trim(), pageable);
    return page.map(CollectionResponse::fromEntity);
  }

  @Transactional
  public void delete(Long publisherId, Long collectionId) {
    Collection collection = findCollection(publisherId, collectionId);
    if (collectionRepository.isReferenced(collectionId)) {
      throw new ConflictException("Collection has books and cannot be deleted");
    }
    collectionRepository.delete(collection);
  }

  // Helper
  private Publisher findPublisher(Long publisherId) {
    return publisherRepository
        .findById(publisherId)
        .orElseThrow(() -> new ResourceNotFoundException("Publisher not found"));
  }

  private Collection findCollection(Long publisherId, Long collectionId) {
    return collectionRepository
        .findByIdAndPublisherId(collectionId, publisherId)
        .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
  }
}
