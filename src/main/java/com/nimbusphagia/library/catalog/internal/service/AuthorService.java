package com.nimbusphagia.library.catalog.internal.service;

import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.contains;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.fullName;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.idNot;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.pattern;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.textEquals;

import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.dto.author.AuthorRequest;
import com.nimbusphagia.library.catalog.internal.dto.author.AuthorResponse;
import com.nimbusphagia.library.catalog.internal.entity.Author;
import com.nimbusphagia.library.catalog.internal.repository.AuthorRepository;
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
public class AuthorService {
  private final AuthorRepository authorRepository;
  private final CatalogAccess access;

  @Transactional
  public AuthorResponse create(Long institutionId, AuthorRequest request) {
    Author author = access.stamp(request.toEntity(), institutionId);
    requireUnique(institutionId, request, null);
    return AuthorResponse.fromEntity(authorRepository.save(author));
  }

  @Transactional
  public AuthorResponse edit(Long institutionId, Long id, AuthorRequest request) {
    Author author = findOwned(institutionId, id);
    requireUnique(institutionId, request, id);
    request.replace(author);
    return AuthorResponse.fromEntity(author);
  }

  @Transactional(readOnly = true)
  public Page<AuthorResponse> findAll(
      Long institutionId, CatalogScope scope, String search, Pageable pageable) {
    Specification<Author> spec = access.in(institutionId, scope);
    if (search != null && !search.isBlank()) {
      spec = spec.and((root, query, cb) -> contains(cb, fullName(cb, root), pattern(search)));
    }
    return authorRepository.findAll(spec, pageable).map(AuthorResponse::fromEntity);
  }

  @Transactional
  public void delete(Long institutionId, Long id) {
    Author author = findOwned(institutionId, id);
    if (authorRepository.isReferenced(id)) {
      throw new ConflictException("Author has books or series and cannot be deleted");
    }
    authorRepository.delete(author);
  }

  // Helper
  private Author findOwned(Long institutionId, Long id) {
    access.requireWriter(institutionId);
    return access
        .owned(authorRepository.findById(id), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
  }

  private void requireUnique(Long institutionId, AuthorRequest request, Long id) {
    if (authorRepository.exists(
        access
            .<Author>visible(institutionId)
            .and(textEquals("firstName", request.firstName()))
            .and(textEquals("lastName", request.lastName()))
            .and(idNot(id)))) {
      throw new ConflictException("Author already exists");
    }
  }
}
