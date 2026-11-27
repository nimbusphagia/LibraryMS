package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.internal.dto.library.LibraryRequest;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelImpact;
import com.nimbusphagia.library.inventory.internal.dto.library.LibraryResponse;
import com.nimbusphagia.library.inventory.internal.entity.LabelScheme;
import com.nimbusphagia.library.inventory.internal.entity.Library;
import com.nimbusphagia.library.inventory.internal.entity.Shelf;
import com.nimbusphagia.library.inventory.internal.repository.LibraryRepository;
import com.nimbusphagia.library.inventory.internal.repository.ShelfRepository;
import com.nimbusphagia.library.organization.AccessGuard;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
@RequiredArgsConstructor
public class LibraryService {
  private static final String DEFAULT_SHELF_NAME = "General";

  private final LibraryRepository libraryRepository;
  private final ShelfRepository shelfRepository;
  private final AccessGuard accessGuard;
  private final LabelSchemeService schemeService;
  private final LabelService labelService;

  @Transactional
  public LibraryResponse create(Long institutionId, LibraryRequest request) {
    if (libraryRepository.existsByInstitutionIdAndNameIgnoreCase(institutionId, request.name())) {
      throw new ConflictException("Library name already exists in this institution");
    }
    LabelScheme scheme =
        request.labelSchemeId() == null
            ? schemeService.defaultTemplate()
            : schemeService.findUsable(request.labelSchemeId());
    Library library = libraryRepository.save(request.toEntity(institutionId, scheme));

    shelfRepository.save(
        Shelf.builder().library(library).name(DEFAULT_SHELF_NAME).defaultShelf(true).build());

    return LibraryResponse.fromEntity(library);
  }

  @Transactional
  public LibraryResponse edit(Long id, LibraryRequest request) {
    Library library = find(id);
    if (libraryRepository.existsByInstitutionIdAndNameIgnoreCaseAndIdNot(
        library.getInstitutionId(), request.name(), id)) {
      throw new ConflictException("Library name already exists in this institution");
    }
    request.replace(library);
    return LibraryResponse.fromEntity(library);
  }

  @Transactional
  public LibraryResponse assignScheme(Long id, Long schemeId) {
    Library library = find(id);
    library.setLabelScheme(schemeService.findUsable(schemeId));
    labelService.recomposeLibrary(id, accessGuard.current().getId());
    return LibraryResponse.fromEntity(library);
  }

  // Assigns the scheme to count the labels it would touch, then rolls everything back
  @Transactional
  public LabelImpact previewScheme(Long id, Long schemeId) {
    find(id).setLabelScheme(schemeService.findUsable(schemeId));
    LabelImpact impact = labelService.recomposeLibrary(id, accessGuard.current().getId());
    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    return impact;
  }

  @Transactional(readOnly = true)
  public List<LibraryResponse> findAll(Boolean active, Long institutionId) {
    AccountPrincipal principal = accessGuard.current();
    if (!principal.isOrganizationWide()) {
      institutionId = principal.getInstitutionId();
    }
    return libraryRepository.search(principal.getOrganizationId(), institutionId, active).stream()
        .map(LibraryResponse::fromEntity)
        .toList();
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
