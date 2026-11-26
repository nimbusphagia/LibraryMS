package com.nimbusphagia.library.organization.internal.repository;

import com.nimbusphagia.library.organization.internal.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {}
