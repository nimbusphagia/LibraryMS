package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.catalog.CatalogApi;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.internal.dto.copy.CopyFilter;
import com.nimbusphagia.library.inventory.internal.dto.copy.CopyMoveRequest;
import com.nimbusphagia.library.inventory.internal.dto.copy.CopyResponse;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;
import com.nimbusphagia.library.inventory.internal.entity.Label;
import com.nimbusphagia.library.inventory.internal.entity.Shelf;
import com.nimbusphagia.library.inventory.internal.repository.CopyRepository;
import com.nimbusphagia.library.inventory.internal.repository.CopySpecifications;
import com.nimbusphagia.library.inventory.internal.repository.LabelRepository;
import com.nimbusphagia.library.inventory.internal.repository.LibraryRepository;
import com.nimbusphagia.library.inventory.internal.repository.ShelfRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CopyService {
  private final LibraryRepository libraryRepository;
  private final ShelfRepository shelfRepository;
  private final CopyRepository copyRepository;
  private final LabelRepository labelRepository;
  private final CatalogApi catalogApi;

  @Transactional(readOnly = true)
  public Page<CopyResponse> findAll(Long libraryId, CopyFilter filter, Pageable pageable) {
    if (!libraryRepository.existsById(libraryId)) {
      throw new ResourceNotFoundException("Library not found");
    }
    Page<Copy> page = copyRepository.findAll(CopySpecifications.from(libraryId, filter), pageable);
    return new PageImpl<>(toResponses(page.getContent()), pageable, page.getTotalElements());
  }

  @Transactional(readOnly = true)
  public CopyResponse findOne(Long libraryId, Long copyId) {
    Copy copy =
        copyRepository
            .findDetailedById(copyId)
            .filter(c -> c.getLibrary().getId().equals(libraryId))
            .orElseThrow(() -> new ResourceNotFoundException("Copy not found in this library"));
    return toResponses(List.of(copy)).get(0);
  }

  @Transactional
  public List<CopyResponse> move(Long libraryId, CopyMoveRequest request) {
    Shelf shelf =
        shelfRepository
            .findByIdAndLibraryId(request.shelfId(), libraryId)
            .orElseThrow(() -> new ResourceNotFoundException("Shelf not found in this library"));

    Set<Long> ids = new HashSet<>();
    for (Long id : request.copyIds()) {
      if (!ids.add(id)) {
        throw new BadRequestException("Copy " + id + " appears more than once");
      }
    }

    Map<Long, Copy> byId = new HashMap<>();
    for (Copy copy : copyRepository.findAllByIdInForUpdate(ids)) {
      byId.put(copy.getId(), copy);
    }

    List<Copy> copies = new ArrayList<>();
    for (Long id : request.copyIds()) {
      Copy copy = byId.get(id);
      if (copy == null || !copy.getLibrary().getId().equals(libraryId)) {
        throw new ResourceNotFoundException("Copy " + id + " not found in this library");
      }
      if (copy.getStatus() != CopyStatus.AVAILABLE) {
        throw new BadRequestException(
            "Copy " + id + " is " + copy.getStatus() + "; only available copies can be moved");
      }
      copies.add(copy);
    }

    for (Copy copy : copies) {
      if (!copy.getShelf().getId().equals(shelf.getId())) {
        copy.setShelf(shelf);
      }
    }
    copyRepository.saveAllAndFlush(copies);

    return toResponses(copies);
  }

  // Helper
  private List<CopyResponse> toResponses(List<Copy> copies) {
    if (copies.isEmpty()) return List.of();

    Set<Long> bookIds = copies.stream().map(c -> c.getBook().getId()).collect(Collectors.toSet());
    Set<Long> copyIds = copies.stream().map(Copy::getId).collect(Collectors.toSet());

    Map<Long, BookLabelData> books = catalogApi.getBookLabelData(bookIds);
    Map<Long, Label> labels =
        labelRepository.findByCopyIdInAndRetiredAtIsNull(copyIds).stream()
            .collect(Collectors.toMap(l -> l.getCopy().getId(), l -> l));

    return copies.stream()
        .map(c -> CopyResponse.of(c, books.get(c.getBook().getId()), labels.get(c.getId())))
        .toList();
  }
}
