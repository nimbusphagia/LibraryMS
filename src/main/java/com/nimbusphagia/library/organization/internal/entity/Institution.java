package com.nimbusphagia.library.organization.internal.entity;

import com.nimbusphagia.library.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "institution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Institution extends BaseEntity {
  @Column(name = "organization_id", nullable = false, updatable = false)
  private Long organizationId;

  @Column(nullable = false)
  private String name;

  // IANA zone id, used to turn loan due dates into end-of-day instants
  @Column(name = "time_zone", nullable = false)
  private String timeZone;

  @Builder.Default
  @Column(nullable = false)
  private boolean active = true;
}
