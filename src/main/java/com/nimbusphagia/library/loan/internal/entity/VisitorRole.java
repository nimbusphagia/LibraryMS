package com.nimbusphagia.library.loan.internal.entity;

import com.nimbusphagia.library.shared.model.ReadOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "visitor_role")
@Getter
@Setter
@NoArgsConstructor
public class VisitorRole extends ReadOnlyEntity {

  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Integer maxDays;

  @Column(nullable = false)
  private Integer maxActiveLoans;
}
