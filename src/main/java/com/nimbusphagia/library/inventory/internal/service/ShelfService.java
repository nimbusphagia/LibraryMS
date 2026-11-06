package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.internal.dto.shelf.ShelfRequest;
import com.nimbusphagia.library.inventory.internal.dto.shelf.ShelfResponse;
import com.nimbusphagia.library.inventory.internal.entity.Library;
import com.nimbusphagia.library.inventory.internal.entity.Shelf;
import com.nimbusphagia.library.inventory.internal.repository.CopyRepository;
import com.nimbusphagia.library.inventory.internal.repository.LibraryRepository;
import com.nimbusphagia.library.inventory.internal.repository.ShelfRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShelfService {
  private final LibraryRepository libraryRepository;
  private final ShelfRepository shelfRepository;
  private final CopyRepository copyRepository;

  @Transactional
  public ShelfResponse create(Long libraryId, ShelfRequest request) {
    Library library =
        libraryRepository
            .findById(libraryId)
            .orElseThrow(() -> new ResourceNotFoundException("Library not found"));
    if (!library.isActive()) {
      throw new BadRequestException("This library is inactive");
    }
    if (shelfRepository.existsByLibraryIdAndNameIgnoreCase(libraryId, request.name())) {
      throw new ConflictException("Shelf name already exists in this library");
    }
    return ShelfResponse.fromEntity(shelfRepository.save(request.toEntity(library)));
  }

  @Transactional
  public ShelfResponse edit(Long libraryId, Long shelfId, ShelfRequest request) {
    Shelf shelf = find(libraryId, shelfId);
    if (shelfRepository.existsByLibraryIdAndNameIgnoreCaseAndIdNot(
        libraryId, request.name(), shelfId)) {
      throw new ConflictException("Shelf name already exists in this library");
    }
    request.replace(shelf);
    return ShelfResponse.fromEntity(shelf);
  }

  @Transactional(readOnly = true)
  public List<ShelfResponse> findAll(Long libraryId) {
    if (!libraryRepository.existsById(libraryId)) {
      throw new ResourceNotFoundException("Library not found");
    }
    return shelfRepository.findByLibraryIdOrderByDefaultShelfDescNameAsc(libraryId).stream()
        .map(ShelfResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public ShelfResponse findOne(Long libraryId, Long shelfId) {
    return ShelfResponse.fromEntity(find(libraryId, shelfId));
  }

  @Transactional
  public ShelfResponse makeDefault(Long libraryId, Long shelfId) {
    Shelf shelf = find(libraryId, shelfId);
    if (shelf.isDefaultShelf()) {
      return ShelfResponse.fromEntity(shelf);
    }

    shelfRepository
        .findByLibraryIdAndDefaultShelfTrue(libraryId)
        .ifPresent(
            current -> {
              current.setDefaultShelf(false);
              shelfRepository.saveAndFlush(current);
            });

    shelf.setDefaultShelf(true);
    shelfRepository.saveAndFlush(shelf);
    return ShelfResponse.fromEntity(shelf);
  }

  @Transactional
  public void delete(Long libraryId, Long shelfId) {
    Shelf shelf = find(libraryId, shelfId);
    if (shelf.isDefaultShelf()) {
      throw new ConflictException("The default shelf cannot be deleted; set another default first");
    }
    if (copyRepository.existsByShelfId(shelfId)) {
      throw new ConflictException("Shelf has copies and cannot be deleted");
    }
    shelfRepository.delete(shelf);
  }

  // Helper
  private Shelf find(Long libraryId, Long shelfId) {
    return shelfRepository
        .findByIdAndLibraryId(shelfId, libraryId)
        .orElseThrow(() -> new ResourceNotFoundException("Shelf not found in this library"));
  }
}
