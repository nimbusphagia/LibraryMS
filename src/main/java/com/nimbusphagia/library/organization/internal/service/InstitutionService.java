package com.nimbusphagia.library.organization.internal.service;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.organization.AccessGuard;
import com.nimbusphagia.library.organization.InstitutionCreatedEvent;
import com.nimbusphagia.library.organization.internal.dto.InstitutionRequest;
import com.nimbusphagia.library.organization.internal.dto.InstitutionResponse;
import com.nimbusphagia.library.organization.internal.entity.Institution;
import com.nimbusphagia.library.organization.internal.repository.InstitutionRepository;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstitutionService {
  private final InstitutionRepository institutionRepository;
  private final AccessGuard accessGuard;
  private final ApplicationEventPublisher events;

  @Transactional(readOnly = true)
  public List<InstitutionResponse> findAll(Boolean active) {
    AccountPrincipal principal = accessGuard.current();
    if (!principal.isOrganizationWide()) {
      return List.of(findOne(principal.getInstitutionId()));
    }
    Long organizationId = principal.getOrganizationId();
    List<Institution> institutions =
        active == null
            ? institutionRepository.findByOrganizationIdOrderByNameAsc(organizationId)
            : institutionRepository.findByOrganizationIdAndActiveOrderByNameAsc(
                organizationId, active);
    return institutions.stream().map(InstitutionResponse::fromEntity).toList();
  }

  @Transactional(readOnly = true)
  public InstitutionResponse findOne(Long id) {
    return InstitutionResponse.fromEntity(find(id));
  }

  @Transactional
  public InstitutionResponse create(InstitutionRequest request) {
    Long organizationId = accessGuard.current().getOrganizationId();
    requireValidZone(request.timeZone());
    if (institutionRepository.existsByOrganizationIdAndNameIgnoreCase(
        organizationId, request.name())) {
      throw new ConflictException("Institution name already exists");
    }
    Institution institution = institutionRepository.save(request.toEntity(organizationId));
    events.publishEvent(new InstitutionCreatedEvent(organizationId, institution.getId()));
    return InstitutionResponse.fromEntity(institution);
  }

  @Transactional
  public InstitutionResponse edit(Long id, InstitutionRequest request) {
    Institution institution = find(id);
    requireValidZone(request.timeZone());
    if (institutionRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
        institution.getOrganizationId(), request.name(), id)) {
      throw new ConflictException("Institution name already exists");
    }
    request.replace(institution);
    return InstitutionResponse.fromEntity(institution);
  }

  @Transactional
  public InstitutionResponse deactivate(Long id) {
    return setActive(id, false);
  }

  @Transactional
  public InstitutionResponse activate(Long id) {
    return setActive(id, true);
  }

  // Helpers
  private InstitutionResponse setActive(Long id, boolean active) {
    Institution institution = find(id);
    institution.setActive(active);
    return InstitutionResponse.fromEntity(institution);
  }

  private Institution find(Long id) {
    return institutionRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Institution not found"));
  }

  private static void requireValidZone(String timeZone) {
    if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
      throw new BadRequestException("Unknown time zone: " + timeZone);
    }
  }
}
