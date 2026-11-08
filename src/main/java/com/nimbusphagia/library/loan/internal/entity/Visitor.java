package com.nimbusphagia.library.loan.internal.entity;

import com.nimbusphagia.library.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "visitor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visitor extends BaseEntity {
  @Column(nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private VisitorRole role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "grade_id")
  private SchoolGrade grade;

  @Builder.Default
  @Column(nullable = false)
  private Boolean active = true;
}
