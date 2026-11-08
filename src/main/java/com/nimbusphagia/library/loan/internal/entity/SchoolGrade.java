package com.nimbusphagia.library.loan.internal.entity;

import com.nimbusphagia.library.shared.model.ReadOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "school_grade")
@Getter
@Setter
@NoArgsConstructor
public class SchoolGrade extends ReadOnlyEntity {
  @Column(nullable = false)
  private Integer grade;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private GradeLevel level;
}
