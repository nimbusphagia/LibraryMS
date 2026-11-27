package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.inventory.internal.entity.Label;
import com.nimbusphagia.library.inventory.internal.entity.LabelValue;
import com.nimbusphagia.library.inventory.internal.entity.PrintStatus;
import java.time.Instant;
import java.util.Map;

public record LabelResponse(
    Long id,
    Long copyId,
    Long bookId,
    String libraryName,
    String bookName,
    Long schemeId,
    Map<LabelPart, LabelValue> parts,
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
        label.getScheme().getId(),
        label.getParts(),
        label.getCopyNumber(),
        label.getText(),
        label.getPrintStatus(),
        label.getPrintedAt(),
        label.getPrintedBy() == null ? null : label.getPrintedBy().getFullName(),
        label.getRevision(),
        label.getRetiredAt());
  }
}
