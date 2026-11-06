package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.inventory.internal.entity.Label;
import com.nimbusphagia.library.inventory.internal.entity.PrintStatus;
import java.time.Instant;

public record LabelResponse(
    Long id,
    Long copyId,
    Long bookId,
    String libraryName,
    String bookName,
    String categoryCode,
    String deweyCode,
    String malagaCode,
    String titleCode,
    Integer collectionVolume,
    Integer seriesVolume,
    Integer copyNumber,
    String text,
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
        label.getDeweyCode(),
        label.getMalagaCode(),
        label.getTitleCode(),
        label.getCollectionVolume(),
        label.getSeriesVolume(),
        label.getCopyNumber(),
        label.getText(),
        label.getPrintStatus(),
        label.getPrintedAt(),
        label.getPrintedBy() == null ? null : label.getPrintedBy().getFullName(),
        label.getRevision(),
        label.getRetiredAt());
  }
}
