package com.nimbusphagia.library_ms.inventory.internal.entity;

import com.nimbusphagia.library_ms.shared.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
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
  String name;
  String description;

  @ManyToOne
  @JoinColumn(name = "shelf_id", nullable = false)
  Library library;

  @OneToMany(mappedBy = "shelf")
  List<Copy> copies;
}
