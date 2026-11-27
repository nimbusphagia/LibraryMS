package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.inventory.internal.entity.LabelRevision;
import com.nimbusphagia.library.inventory.internal.entity.LabelValue;
import java.time.Instant;
import java.util.Map;

public record LabelRevisionResponse(
    Long id,
    int revisionNumber,
    Long schemeId,
    Map<LabelPart, LabelValue> parts,
    Integer copyNumber,
    String text,
    String changedBy,
    String changeReason,
    Instant createdAt) {

  // Transaction only
  public static LabelRevisionResponse fromEntity(LabelRevision r) {
    return new LabelRevisionResponse(
        r.getId(),
        r.getRevisionNumber(),
        r.getScheme().getId(),
        r.getParts(),
        r.getCopyNumber(),
        r.getText(),
        r.getChangedBy().getFullName(),
        r.getChangeReason(),
        r.getCreatedAt());
  }
}
