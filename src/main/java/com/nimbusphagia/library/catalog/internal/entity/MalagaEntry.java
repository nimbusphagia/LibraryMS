package com.nimbusphagia.library.catalog.internal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "malaga_entry")
@Immutable
@Getter
public class MalagaEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String letter;

  @Column(nullable = false)
  private String entry;

  private String qualifier;

  @Column(nullable = false)
  private Integer number;
}
