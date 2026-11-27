package com.nimbusphagia.library.inventory.internal.entity;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.catalog.internal.entity.Book;
import com.nimbusphagia.library.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "label")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Label extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "library_id", nullable = false)
  private Library library;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "copy_id", nullable = false, unique = true)
  private Copy copy;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "scheme_id", nullable = false)
  private LabelScheme scheme;

  // Every part of the scheme that has a value, except the copy number
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private Map<LabelPart, LabelValue> parts;

  @Column(nullable = false)
  private Integer copyNumber; // Ejemplar

  @Column(nullable = false)
  private String text;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PrintStatus printStatus = PrintStatus.PENDING_PRINT;

  private Instant printedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "printed_by_id")
  private Account printedBy;

  @Builder.Default
  @Column(nullable = false)
  private int revision = 1;

  private Instant retiredAt;
}
