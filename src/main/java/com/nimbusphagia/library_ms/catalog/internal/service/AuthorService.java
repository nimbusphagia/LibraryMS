package com.nimbusphagia.library_ms.catalog.internal.service;

import com.nimbusphagia.library_ms.catalog.internal.dto.author.AuthorRequest;
import com.nimbusphagia.library_ms.catalog.internal.dto.author.AuthorResponse;
import com.nimbusphagia.library_ms.catalog.internal.entity.Author;
import com.nimbusphagia.library_ms.catalog.internal.repository.AuthorRepository;
import com.nimbusphagia.library_ms.exception.custom.ConflictException;
import com.nimbusphagia.library_ms.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthorService {
  private final AuthorRepository authorRepository;

  @Transactional
  public AuthorResponse create(AuthorRequest request) {
    if (authorRepository.existsByFullName(request.firstName(), request.lastName())) {
      throw new ConflictException("Author already exists");
    }
    Author author = authorRepository.save(request.toEntity());
    return AuthorResponse.fromEntity(author);
  }

  @Transactional
  public AuthorResponse edit(Long id, AuthorRequest request) {
    Author author =
        authorRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
    if (authorRepository.existsByFullNameAndIdNot(request.firstName(), request.lastName(), id)) {
      throw new ConflictException("Author already exists");
    }
    request.replace(author);
    return AuthorResponse.fromEntity(author);
  }

  @Transactional(readOnly = true)
  public Page<AuthorResponse> findAll(String search, Pageable pageable) {
    Page<Author> page =
        (search == null || search.isBlank())
            ? authorRepository.findAll(pageable)
            : authorRepository.search(search.trim(), pageable);
    return page.map(AuthorResponse::fromEntity);
  }

  @Transactional
  public void delete(Long id) {
    Author author =
        authorRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
    if (authorRepository.isReferenced(id)) {
      throw new ConflictException("Author has books and cannot be deleted");
    }
    authorRepository.delete(author);
  }
}
