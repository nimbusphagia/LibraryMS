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
@Table(name = "author")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author extends BaseEntity {
  @Column(nullable = false)
  private String firstName;

  private String lastName;

  private String code; // COMPUTED: notacion malaga
}
