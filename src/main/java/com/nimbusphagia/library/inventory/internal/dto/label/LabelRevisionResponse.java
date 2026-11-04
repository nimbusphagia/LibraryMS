package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.inventory.internal.entity.LabelRevision;
import com.nimbusphagia.library.inventory.internal.entity.NotationSource;
import java.time.Instant;

public record LabelRevisionResponse(
    Long id,
    int revisionNumber,
    String categoryCode,
    String collectionCode,
    String deweyCode,
    NotationSource malagaSource,
    String malagaCode,
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
        r.getCollectionCode(),
        r.getDeweyCode(),
        r.getMalagaSource(),
        r.getMalagaCode(),
        r.getCollectionVolume(),
        r.getSeriesVolume(),
        r.getCopyNumber(),
        r.getText(),
        r.getChangedBy().getFullName(),
        r.getChangeReason(),
        r.getCreatedAt());
  }
}
