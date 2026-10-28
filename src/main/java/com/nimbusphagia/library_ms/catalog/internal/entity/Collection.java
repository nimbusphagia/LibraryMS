package com.nimbusphagia.library_ms.catalog.internal.entity;

import com.nimbusphagia.library_ms.shared.model.BaseEntity;
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
@Table(name = "collection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collection extends BaseEntity {
  @Column(nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "publisher_id", nullable = false)
  private Publisher publisher;
}
