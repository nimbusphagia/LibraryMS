package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.internal.dto.library.LibraryRequest;
import com.nimbusphagia.library.inventory.internal.dto.library.LibraryResponse;
import com.nimbusphagia.library.inventory.internal.entity.Library;
import com.nimbusphagia.library.inventory.internal.entity.Shelf;
import com.nimbusphagia.library.inventory.internal.repository.LibraryRepository;
import com.nimbusphagia.library.inventory.internal.repository.ShelfRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryService {
  private static final String DEFAULT_SHELF_NAME = "General";

  private final LibraryRepository libraryRepository;
  private final ShelfRepository shelfRepository;

  @Transactional
  public LibraryResponse create(LibraryRequest request) {
    if (libraryRepository.existsByNameIgnoreCase(request.name())) {
      throw new ConflictException("Library name already exists");
    }
    Library library = libraryRepository.save(request.toEntity());

    shelfRepository.save(
        Shelf.builder().library(library).name(DEFAULT_SHELF_NAME).defaultShelf(true).build());

    return LibraryResponse.fromEntity(library);
  }

  @Transactional
  public LibraryResponse edit(Long id, LibraryRequest request) {
    Library library = find(id);
    if (libraryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
      throw new ConflictException("Library name already exists");
    }
    request.replace(library);
    return LibraryResponse.fromEntity(library);
  }

  @Transactional(readOnly = true)
  public List<LibraryResponse> findAll(Boolean active) {
    List<Library> libraries =
        active == null
            ? libraryRepository.findAllByOrderByNameAsc()
            : libraryRepository.findByActiveOrderByNameAsc(active);
    return libraries.stream().map(LibraryResponse::fromEntity).toList();
  }

  @Transactional(readOnly = true)
  public LibraryResponse findOne(Long id) {
    return LibraryResponse.fromEntity(find(id));
  }

  @Transactional
  public LibraryResponse deactivate(Long id) {
    return setActive(id, false);
  }

  @Transactional
  public LibraryResponse activate(Long id) {
    return setActive(id, true);
  }

  // Helpers
  private LibraryResponse setActive(Long id, boolean active) {
    Library library = find(id);
    library.setActive(active);
    return LibraryResponse.fromEntity(library);
  }

  private Library find(Long id) {
    return libraryRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Library not found"));
  }
}
