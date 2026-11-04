package com.nimbusphagia.library.inventory.internal.entity;

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
@Table(name = "shelf")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shelf extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "library_id", nullable = false)
  private Library library;

  @Column(nullable = false)
  private String name;

  private String description;

  @Builder.Default
  @Column(name = "is_default", nullable = false)
  private boolean defaultShelf = false;
}
