package com.nimbusphagia.library.inventory.internal.entity;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.shared.model.ReadOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "label_revision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabelRevision extends ReadOnlyEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "label_id", nullable = false)
  private Label label;

  @Column(nullable = false)
  private Integer revisionNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "scheme_id", nullable = false)
  private LabelScheme scheme;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private Map<LabelPart, LabelValue> parts;

  @Column(nullable = false)
  private Integer copyNumber;

  @Column(nullable = false)
  private String text;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "changed_by_id", nullable = false)
  private Account changedBy;

  @Column(columnDefinition = "text")
  private String changeReason;
}
