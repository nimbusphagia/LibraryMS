package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ForbiddenException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelImpact;
import com.nimbusphagia.library.inventory.internal.dto.scheme.LabelSchemeRequest;
import com.nimbusphagia.library.inventory.internal.dto.scheme.LabelSchemeResponse;
import com.nimbusphagia.library.inventory.internal.entity.LabelLayout;
import com.nimbusphagia.library.inventory.internal.entity.LabelLayout.PartRule;
import com.nimbusphagia.library.inventory.internal.entity.LabelScheme;
import com.nimbusphagia.library.inventory.internal.repository.LabelSchemeRepository;
import com.nimbusphagia.library.inventory.internal.repository.LibraryRepository;
import com.nimbusphagia.library.organization.AccessGuard;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
@RequiredArgsConstructor
public class LabelSchemeService {
  static final String DEFAULT_TEMPLATE = "Spring";

  private final LabelSchemeRepository schemeRepository;
  private final LibraryRepository libraryRepository;
  private final AccessGuard accessGuard;
  private final LabelService labelService;

  @Transactional(readOnly = true)
  public List<LabelSchemeResponse> findAll() {
    return schemeRepository.findUsableBy(organizationId()).stream()
        .map(LabelSchemeResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public LabelSchemeResponse findOne(Long id) {
    return LabelSchemeResponse.fromEntity(findUsable(id));
  }

  @Transactional
  public LabelSchemeResponse create(LabelSchemeRequest request) {
    requireValid(request.layout());
    requireUniqueName(request.name(), null);
    LabelScheme scheme =
        LabelScheme.builder()
            .organizationId(organizationId())
            .name(request.name())
            .layout(request.layout())
            .build();
    return LabelSchemeResponse.fromEntity(schemeRepository.save(scheme));
  }

  @Transactional
  public LabelSchemeResponse edit(Long id, LabelSchemeRequest request) {
    LabelScheme scheme = change(id, request);
    labelService.recomposeScheme(id, actorId());
    return LabelSchemeResponse.fromEntity(scheme);
  }

  // Applies the edit to count the labels it would touch, then rolls everything back
  @Transactional
  public LabelImpact preview(Long id, LabelSchemeRequest request) {
    change(id, request);
    LabelImpact impact = labelService.recomposeScheme(id, actorId());
    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    return impact;
  }

  @Transactional
  public void delete(Long id) {
    LabelScheme scheme = findEditable(id);
    if (libraryRepository.existsByLabelSchemeId(id)) {
      throw new ConflictException("The scheme is assigned to a library and cannot be deleted");
    }
    schemeRepository.delete(scheme);
  }

  // A template or a scheme of the current organization
  LabelScheme findUsable(Long id) {
    return schemeRepository
        .findById(id)
        .filter(s -> s.isTemplate() || s.getOrganizationId().equals(organizationId()))
        .orElseThrow(() -> new ResourceNotFoundException("Label scheme not found"));
  }

  LabelScheme defaultTemplate() {
    return schemeRepository.findByOrganizationIdIsNullAndName(DEFAULT_TEMPLATE).orElseThrow();
  }

  // Helpers
  private LabelScheme change(Long id, LabelSchemeRequest request) {
    LabelScheme scheme = findEditable(id);
    requireValid(request.layout());
    requireUniqueName(request.name(), id);
    scheme.setName(request.name());
    scheme.setLayout(request.layout());
    return scheme;
  }

  private LabelScheme findEditable(Long id) {
    LabelScheme scheme = findUsable(id);
    if (scheme.isTemplate()) {
      throw new ForbiddenException("Templates are read-only; copy one into a new scheme");
    }
    return scheme;
  }

  private void requireUniqueName(String name, Long id) {
    if (schemeRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
        organizationId(), name, id == null ? 0L : id)) {
      throw new ConflictException("Label scheme name already exists");
    }
  }

  private static void requireValid(LabelLayout layout) {
    Set<LabelPart> seen = EnumSet.noneOf(LabelPart.class);
    for (PartRule rule : layout.rules().toList()) {
      if (!seen.add(rule.part())) {
        throw new BadRequestException("Each part may appear only once: " + rule.part());
      }
    }
    PartRule copyNumber =
        layout
            .rules()
            .filter(r -> r.part() == LabelPart.COPY_NUMBER)
            .findFirst()
            .orElseThrow(() -> new BadRequestException("The copy number is always required"));
    if (!copyNumber.required() || copyNumber.editable()) {
      throw new BadRequestException("The copy number must be required and not editable");
    }
    if (layout.rules().noneMatch(r -> r.required() && r.part() != LabelPart.COPY_NUMBER)) {
      throw new BadRequestException("Another part besides the copy number must be required");
    }
    // It is never derived, so its value is always written by hand
    if (layout.rules().anyMatch(r -> r.part() == LabelPart.AUTHOR_MALAGA && !r.editable())) {
      throw new BadRequestException("The Malaga code must be editable");
    }
  }

  private Long actorId() {
    return accessGuard.current().getId();
  }

  private Long organizationId() {
    return accessGuard.current().getOrganizationId();
  }
}
