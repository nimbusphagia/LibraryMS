package com.nimbusphagia.library_ms.inventory.internal.entity;

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
@Table(name = "library")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Library extends BaseEntity {

  String name;

  @ManyToOne
  @JoinColumn(name = "classification_id", nullable = false)
  ClassificationSystem classificationSystem;
}
