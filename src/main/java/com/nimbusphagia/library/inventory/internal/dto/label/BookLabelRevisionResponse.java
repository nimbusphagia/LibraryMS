package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.inventory.internal.entity.BookLabelRevision;
import com.nimbusphagia.library.inventory.internal.entity.LabelValue;
import java.time.Instant;
import java.util.Map;

public record BookLabelRevisionResponse(
    Long id,
    int revisionNumber,
    Long schemeId,
    Map<LabelPart, LabelValue> parts,
    String code,
    String changedBy,
    String changeReason,
    Instant createdAt) {

  // Transaction only
  public static BookLabelRevisionResponse fromEntity(BookLabelRevision r) {
    return new BookLabelRevisionResponse(
        r.getId(),
        r.getRevisionNumber(),
        r.getScheme().getId(),
        r.getParts(),
        r.getCode(),
        r.getChangedBy().getFullName(),
        r.getChangeReason(),
        r.getCreatedAt());
  }
}
