package com.nimbusphagia.library.inventory.internal.entity;

import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.catalog.internal.entity.Book;
import com.nimbusphagia.library.inventory.internal.util.LabelTextComposer;
import com.nimbusphagia.library.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.EnumMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A book's label pattern in a library; each copy prints it with its own number. */
@Entity
@Table(name = "book_label")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookLabel extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "library_id", nullable = false, updatable = false)
  private Library library;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false, updatable = false)
  private Book book;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "scheme_id", nullable = false)
  private LabelScheme scheme;

  // The scheme's layout when the pattern was last built; the scheme may have changed since
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private LabelLayout layout;

  // Every part of the scheme that has a value, except the copy number
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private Map<LabelPart, LabelValue> parts;

  @Column(nullable = false)
  private String code;

  @Builder.Default
  @Column(nullable = false)
  private int revision = 1;

  @Builder.Default
  @Column(nullable = false)
  private boolean outdated = false;

  public String textFor(int copyNumber) {
    return LabelTextComposer.compose(layout, values(parts), copyNumber);
  }

  public static Map<LabelPart, String> values(Map<LabelPart, LabelValue> parts) {
    Map<LabelPart, String> values = new EnumMap<>(LabelPart.class);
    parts.forEach((part, value) -> values.put(part, value.value()));
    return values;
  }
}
