package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.inventory.internal.entity.Label;
import com.nimbusphagia.library.inventory.internal.entity.NotationSource;
import com.nimbusphagia.library.inventory.internal.entity.PrintStatus;
import java.time.Instant;

public record LabelResponse(
    Long id,
    Long copyId,
    Long bookId,
    String libraryName,
    String bookName,
    String categoryCode,
    String collectionCode,
    String deweyCode,
    NotationSource malagaSource,
    String malagaCode,
    Integer collectionVolume,
    Integer seriesVolume,
    Integer copyNumber,
    String text,
    boolean customized,
    PrintStatus printStatus,
    Instant printedAt,
    String printedBy,
    int revision,
    Instant retiredAt) {

  // Transaction only
  public static LabelResponse fromEntity(Label label) {
    return new LabelResponse(
        label.getId(),
        label.getCopy().getId(),
        label.getBook().getId(),
        label.getLibrary().getName(),
        label.getBook().getTitle(),
        label.getCategoryCode(),
        label.getCollectionCode(),
        label.getDeweyCode(),
        label.getMalagaSource(),
        label.getMalagaCode(),
        label.getCollectionVolume(),
        label.getSeriesVolume(),
        label.getCopyNumber(),
        label.getText(),
        label.isCustomized(),
        label.getPrintStatus(),
        label.getPrintedAt(),
        label.getPrintedBy() == null ? null : label.getPrintedBy().getFullName(),
        label.getRevision(),
        label.getRetiredAt());
  }
}
