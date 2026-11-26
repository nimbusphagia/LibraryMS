package com.nimbusphagia.library.catalog.internal.service;

import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.anyContains;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.attributeIs;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.idNot;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.textEquals;

import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.dto.collection.CollectionRequest;
import com.nimbusphagia.library.catalog.internal.dto.collection.CollectionResponse;
import com.nimbusphagia.library.catalog.internal.entity.Collection;
import com.nimbusphagia.library.catalog.internal.entity.Publisher;
import com.nimbusphagia.library.catalog.internal.repository.CollectionRepository;
import com.nimbusphagia.library.catalog.internal.repository.PublisherRepository;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CollectionService {
  private final CollectionRepository collectionRepository;
  private final PublisherRepository publisherRepository;
  private final CatalogAccess access;

  @Transactional
  public CollectionResponse create(
      Long institutionId, Long publisherId, CollectionRequest request) {
    Publisher publisher = findPublisher(institutionId, publisherId);
    Collection collection = access.stamp(request.toEntity(publisher), institutionId);
    requireUnique(institutionId, publisherId, request, null);
    return CollectionResponse.fromEntity(collectionRepository.save(collection));
  }

  @Transactional
  public CollectionResponse edit(
      Long institutionId, Long publisherId, Long collectionId, CollectionRequest request) {
    Collection collection = findOwned(institutionId, publisherId, collectionId);
    requireUnique(institutionId, publisherId, request, collectionId);
    request.replace(collection);
    return CollectionResponse.fromEntity(collection);
  }

  @Transactional(readOnly = true)
  public Page<CollectionResponse> findAll(
      Long institutionId,
      Long publisherId,
      CatalogScope scope,
      String search,
      Pageable pageable) {
    findPublisher(institutionId, publisherId);
    return collectionRepository
        .findAll(
            access
                .<Collection>in(institutionId, scope)
                .and(attributeIs("publisher", publisherId))
                .and(anyContains(search, "name", "code")),
            pageable)
        .map(CollectionResponse::fromEntity);
  }

  @Transactional
  public void delete(Long institutionId, Long publisherId, Long collectionId) {
    Collection collection = findOwned(institutionId, publisherId, collectionId);
    if (collectionRepository.isReferenced(collectionId)) {
      throw new ConflictException("Collection has books and cannot be deleted");
    }
    collectionRepository.delete(collection);
  }

  // Helper
  private Publisher findPublisher(Long institutionId, Long publisherId) {
    return access
        .visible(publisherRepository.findById(publisherId), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Publisher not found"));
  }

  private Collection findOwned(Long institutionId, Long publisherId, Long collectionId) {
    access.requireWriter(institutionId);
    return access
        .owned(
            collectionRepository.findByIdAndPublisherId(collectionId, publisherId), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
  }

  private void requireUnique(
      Long institutionId, Long publisherId, CollectionRequest request, Long id) {
    Specification<Collection> sameScope =
        access.<Collection>visible(institutionId).and(attributeIs("publisher", publisherId));
    if (collectionRepository.exists(
        sameScope.and(textEquals("name", request.name())).and(idNot(id)))) {
      throw new ConflictException("Collection name already exists for this publisher");
    }
    if (request.code() != null
        && collectionRepository.exists(
            sameScope.and(textEquals("code", request.code())).and(idNot(id)))) {
      throw new ConflictException("Collection code already exists for this publisher");
    }
  }
}
