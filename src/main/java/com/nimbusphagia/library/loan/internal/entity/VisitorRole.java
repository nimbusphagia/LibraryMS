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
@Table(name = "visitor_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VisitorRole extends BaseEntity {
  @Column(name = "organization_id", nullable = false, updatable = false)
  private Long organizationId;

  // Null for the organization's templates
  @Column(name = "institution_id", updatable = false)
  private Long institutionId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private boolean requiresGrade;

  @Column(nullable = false)
  private Integer maxDays;

  @Column(nullable = false)
  private Integer maxActiveLoans;

  // Overdue copies in a library that block new loans there
  @Column(nullable = false)
  private Integer overdueBlockThreshold;

  // Days past the due date before a copy counts as overdue for the block
  @Column(nullable = false)
  private Integer graceDays;
}
