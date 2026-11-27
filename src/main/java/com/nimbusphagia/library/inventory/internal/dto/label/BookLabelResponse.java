package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.inventory.internal.entity.BookLabel;
import com.nimbusphagia.library.inventory.internal.entity.LabelValue;
import java.util.Map;

public record BookLabelResponse(
    Long id,
    Long bookId,
    String bookName,
    Long schemeId,
    Map<LabelPart, LabelValue> parts,
    String code,
    int revision,
    boolean outdated) {

  // Transaction only
  public static BookLabelResponse fromEntity(BookLabel pattern) {
    return new BookLabelResponse(
        pattern.getId(),
        pattern.getBook().getId(),
        pattern.getBook().getTitle(),
        pattern.getScheme().getId(),
        pattern.getParts(),
        pattern.getCode(),
        pattern.getRevision(),
        pattern.isOutdated());
  }
}
