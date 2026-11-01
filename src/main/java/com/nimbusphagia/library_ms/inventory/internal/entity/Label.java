package com.nimbusphagia.library_ms.inventory.internal.entity;

import com.nimbusphagia.library_ms.account.internal.entity.Account;
import com.nimbusphagia.library_ms.catalog.internal.entity.Book;
import com.nimbusphagia.library_ms.shared.model.BaseEntity;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

  private String categoryCode;

  private String collectionCode;

  @Column(nullable = false)
  private String deweyCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotationSource notationSource;

  @Column(nullable = false)
  private String notation;

  private Integer collectionVolume; // Volumen

  private Integer seriesVolume; // Tomo

  @Column(nullable = false)
  private Integer copyNumber; // Ejemplar

  @Column(nullable = false)
  private String text;

  @Builder.Default
  @Column(nullable = false)
  private boolean customized = false;

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
}
