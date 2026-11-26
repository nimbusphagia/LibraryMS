package com.nimbusphagia.library.loan.internal.entity;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.shared.model.ReadOnlyEntity;
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

@Entity
@Table(name = "visitor_incident")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitorIncident extends ReadOnlyEntity {
  @Column(name = "institution_id", nullable = false, updatable = false)
  private Long institutionId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "visitor_id", nullable = false, updatable = false)
  private Visitor visitor;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "type_id", nullable = false, updatable = false)
  private VisitorIncidentType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "loan_line_id", updatable = false)
  private LoanMovementLine loanLine;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recorded_by_id", nullable = false, updatable = false)
  private Account recordedBy;

  @Column(columnDefinition = "text", updatable = false)
  private String notes;
}
