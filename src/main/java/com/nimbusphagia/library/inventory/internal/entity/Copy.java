package com.nimbusphagia.library.inventory.internal.entity;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.catalog.internal.entity.Book;
import com.nimbusphagia.library.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
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
  private Integer iteration;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CopyStatus status = CopyStatus.AVAILABLE;

  private Instant damagedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "damaged_by_id")
  private Account damagedBy;

  // The copy in the origin library this one was transferred from
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transferred_from_copy_id")
  private Copy transferredFrom;

  @Column(columnDefinition = "text")
  private String notes;

  // The label stuck on it; its state comes from comparing it with the book's pattern
  private String placedText;

  private Instant placedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placed_by_id")
  private Account placedBy;

  public boolean isDamaged() {
    return damagedAt != null;
  }

  public void place(String text, Instant at, Account by) {
    placedText = text;
    placedAt = at;
    placedBy = by;
  }

  public void clearPlaced() {
    place(null, null, null);
  }
}
