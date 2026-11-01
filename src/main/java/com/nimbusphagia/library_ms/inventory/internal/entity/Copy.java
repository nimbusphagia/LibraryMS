package com.nimbusphagia.library_ms.inventory.internal.entity;

import com.nimbusphagia.library_ms.catalog.internal.entity.Book;
import com.nimbusphagia.library_ms.shared.model.BaseEntity;
import jakarta.persistence.Entity;
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
  String publicId;

  String label;

  @ManyToOne
  @JoinColumn(name = "shelf_id")
  Shelf shelf;

  @ManyToOne
  @JoinColumn(name = "book_id")
  Book book;
}
