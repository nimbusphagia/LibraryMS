package com.nimbusphagia.library_ms.catalog.internal.entity;

import com.nimbusphagia.library_ms.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dewey_tag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeweyTag extends BaseEntity {
  private String name;

  @Column(nullable = false)
  private String code;

  private String description;
}
