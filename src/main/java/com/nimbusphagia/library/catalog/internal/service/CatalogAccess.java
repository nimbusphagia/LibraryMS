package com.nimbusphagia.library.catalog.internal.service;

import com.nimbusphagia.library.catalog.internal.dto.CatalogScope;
import com.nimbusphagia.library.catalog.internal.entity.CatalogEntity;
import com.nimbusphagia.library.exception.ErrorCodes;
import com.nimbusphagia.library.exception.custom.ForbiddenException;
import com.nimbusphagia.library.organization.AccessGuard;
import jakarta.persistence.criteria.Predicate;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Scope rules of the catalog. A null institution id is the shared catalog: the whole organization
 * reads it and only admins write it. An institution's exclusive rows may use shared rows, never
 * the other way around. Routes under an institution are already checked by the ScopeInterceptor.
 */
@Component
@RequiredArgsConstructor
public class CatalogAccess {
  private final AccessGuard accessGuard;

  public boolean isAdmin() {
    return accessGuard.hasRole("ADMIN");
  }

  public void requireWriter(Long institutionId) {
    if (institutionId == null && !isAdmin()) {
      throw new ForbiddenException(
          ErrorCodes.REQUIRES_ADMIN, "Only an admin can change the shared catalog");
    }
  }

  public <T extends CatalogEntity> T stamp(T entity, Long institutionId) {
    requireWriter(institutionId);
    entity.setOrganizationId(organizationId());
    entity.setInstitutionId(institutionId);
    return entity;
  }

  // The row that lives in exactly this scope, the only one its routes may change
  public <T extends CatalogEntity> Optional<T> owned(Optional<T> entity, Long institutionId) {
    Long organizationId = organizationId();
    return entity.filter(
        e ->
            e.getOrganizationId().equals(organizationId)
                && Objects.equals(e.getInstitutionId(), institutionId));
  }

  // A row this scope can read or reference: shared, or exclusive to the same institution
  public <T extends CatalogEntity> Optional<T> visible(Optional<T> entity, Long institutionId) {
    return entity.filter(e -> isVisible(e, institutionId));
  }

  public boolean isVisible(CatalogEntity entity, Long institutionId) {
    return entity.getOrganizationId().equals(organizationId())
        && (entity.getInstitutionId() == null
            || entity.getInstitutionId().equals(institutionId));
  }

  public <T extends CatalogEntity> Specification<T> visible(Long institutionId) {
    return in(institutionId, CatalogScope.ALL);
  }

  // Shared routes have no exclusive part, whatever the requested scope
  public <T extends CatalogEntity> Specification<T> in(Long institutionId, CatalogScope scope) {
    Long organizationId = organizationId();
    CatalogScope effective = institutionId == null ? CatalogScope.SHARED : scope;
    return (root, query, cb) -> {
      Predicate shared = cb.isNull(root.get("institutionId"));
      Predicate exclusive = cb.equal(root.get("institutionId"), institutionId);
      Predicate part =
          switch (effective) {
            case SHARED -> shared;
            case EXCLUSIVE -> exclusive;
            case ALL -> cb.or(shared, exclusive);
          };
      return cb.and(cb.equal(root.get("organizationId"), organizationId), part);
    };
  }

  public Long organizationId() {
    return accessGuard.current().getOrganizationId();
  }
}
