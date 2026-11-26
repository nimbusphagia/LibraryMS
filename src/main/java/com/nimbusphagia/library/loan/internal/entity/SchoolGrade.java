package com.nimbusphagia.library.loan.internal.entity;

import com.nimbusphagia.library.shared.model.ReadOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "school_grade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SchoolGrade extends ReadOnlyEntity {
  @Column(name = "organization_id", nullable = false, updatable = false)
  private Long organizationId;

  // Null for the organization's templates
  @Column(name = "institution_id", updatable = false)
  private Long institutionId;

  @Column(nullable = false)
  private Integer grade;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private GradeLevel level;
}
