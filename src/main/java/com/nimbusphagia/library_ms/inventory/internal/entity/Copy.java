package com.nimbusphagia.library_ms.inventory.internal.entity;

import com.nimbusphagia.library_ms.catalog.internal.entity.Book;
import com.nimbusphagia.library_ms.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "copy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Copy extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "library_id", nullable = false)
  private Library library;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "shelf_id", nullable = false)
  private Shelf shelf;

  @Column(nullable = false)
  private String code;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CopyStatus status = CopyStatus.AVAILABLE;

  @Column(columnDefinition = "text")
  private String notes;
}
