package com.nimbusphagia.library.organization.internal.service;

import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.organization.OrganizationApi;
import com.nimbusphagia.library.organization.OrganizationCreatedEvent;
import com.nimbusphagia.library.organization.internal.entity.Organization;
import com.nimbusphagia.library.organization.internal.repository.InstitutionRepository;
import com.nimbusphagia.library.organization.internal.repository.OrganizationRepository;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationApiImpl implements OrganizationApi {
  private final OrganizationRepository organizationRepository;
  private final InstitutionRepository institutionRepository;
  private final ApplicationEventPublisher events;

  @Override
  @Transactional
  public Long createOrganization(String name) {
    Long id = organizationRepository.save(Organization.builder().name(name).build()).getId();
    events.publishEvent(new OrganizationCreatedEvent(id));
    return id;
  }

  @Override
  @Transactional(readOnly = true)
  public ZoneId zoneOfLibrary(Long libraryId) {
    return institutionRepository
        .findScopeOfLibrary(libraryId)
        .map(s -> ZoneId.of(s.getTimeZone()))
        .orElseThrow(() -> new ResourceNotFoundException("Library not found"));
  }
}
