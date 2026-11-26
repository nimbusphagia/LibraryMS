package com.nimbusphagia.library.catalog.internal.service;

import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.anyContains;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.idNot;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.textEquals;

import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.dto.publisher.PublisherRequest;
import com.nimbusphagia.library.catalog.internal.dto.publisher.PublisherResponse;
import com.nimbusphagia.library.catalog.internal.entity.Publisher;
import com.nimbusphagia.library.catalog.internal.repository.PublisherRepository;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublisherService {
  private final PublisherRepository publisherRepository;
  private final CatalogAccess access;

  @Transactional
  public PublisherResponse create(Long institutionId, PublisherRequest request) {
    Publisher publisher = access.stamp(request.toEntity(), institutionId);
    requireUniqueName(institutionId, request.name(), null);
    return PublisherResponse.fromEntity(publisherRepository.save(publisher));
  }

  @Transactional
  public PublisherResponse edit(Long institutionId, Long id, PublisherRequest request) {
    Publisher publisher = findOwned(institutionId, id);
    requireUniqueName(institutionId, request.name(), id);
    request.replace(publisher);
    return PublisherResponse.fromEntity(publisher);
  }

  @Transactional(readOnly = true)
  public Page<PublisherResponse> findAll(
      Long institutionId, CatalogScope scope, String search, Pageable pageable) {
    return publisherRepository
        .findAll(
            access.<Publisher>in(institutionId, scope).and(anyContains(search, "name")), pageable)
        .map(PublisherResponse::fromEntity);
  }

  @Transactional
  public void delete(Long institutionId, Long id) {
    Publisher publisher = findOwned(institutionId, id);
    if (publisherRepository.isReferenced(id)) {
      throw new ConflictException("Publisher has books or collections and cannot be deleted");
    }
    publisherRepository.delete(publisher);
  }

  // Helper
  private Publisher findOwned(Long institutionId, Long id) {
    access.requireWriter(institutionId);
    return access
        .owned(publisherRepository.findById(id), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Publisher not found"));
  }

  private void requireUniqueName(Long institutionId, String name, Long id) {
    if (publisherRepository.exists(
        access.<Publisher>visible(institutionId).and(textEquals("name", name)).and(idNot(id)))) {
      throw new ConflictException("Publisher name already exists");
    }
  }
}
