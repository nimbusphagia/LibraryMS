package com.nimbusphagia.library.catalog.internal.entity;

import com.nimbusphagia.library.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/** A catalog row of one organization. A null institution means the shared catalog. */
@MappedSuperclass
@Getter
@Setter
public abstract class CatalogEntity extends BaseEntity {
  @Column(name = "organization_id", nullable = false, updatable = false)
  private Long organizationId;

  // Fixed at creation: an exclusive row never moves to the shared catalog
  @Column(name = "institution_id", updatable = false)
  private Long institutionId;
}
