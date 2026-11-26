package com.nimbusphagia.library.organization.internal.service;

import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.organization.OrganizationApi;
import com.nimbusphagia.library.organization.internal.entity.Organization;
import com.nimbusphagia.library.organization.internal.repository.InstitutionRepository;
import com.nimbusphagia.library.organization.internal.repository.OrganizationRepository;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationApiImpl implements OrganizationApi {
  private final OrganizationRepository organizationRepository;
  private final InstitutionRepository institutionRepository;

  @Override
  @Transactional
  public Long createOrganization(String name) {
    return organizationRepository.save(Organization.builder().name(name).build()).getId();
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
