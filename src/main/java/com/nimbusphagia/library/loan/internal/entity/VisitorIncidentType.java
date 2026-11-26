package com.nimbusphagia.library.loan.internal.entity;

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
@Table(name = "visitor_incident_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitorIncidentType extends BaseEntity {
  @Column(name = "institution_id", nullable = false, updatable = false)
  private Long institutionId;

  @Column(nullable = false)
  private String name;

  // Reputation points; a late return is 1, damage 2 and a loss 4
  @Column(nullable = false)
  private Integer weight;

  // Counts like a loss towards AT_RISK
  @Column(nullable = false)
  private boolean serious;

  // Inactive types keep their incidents but cannot be used for new ones
  @Builder.Default
  @Column(nullable = false)
  private boolean active = true;
}
