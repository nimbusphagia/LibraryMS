package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.inventory.internal.entity.LabelRevision;
import java.time.Instant;

public record LabelRevisionResponse(
    Long id,
    int revisionNumber,
    String categoryCode,
    String deweyCode,
    String malagaCode,
    String titleCode,
    Integer collectionVolume,
    Integer seriesVolume,
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
        r.getCategoryCode(),
        r.getDeweyCode(),
        r.getMalagaCode(),
        r.getTitleCode(),
        r.getCollectionVolume(),
        r.getSeriesVolume(),
        r.getCopyNumber(),
        r.getText(),
        r.getChangedBy().getFullName(),
        r.getChangeReason(),
        r.getCreatedAt());
  }
}
