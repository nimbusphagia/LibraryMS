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
    Integer copyNumber,
    String text,
    PrintStatus printStatus,
    Instant printedAt,
    String printedBy,
    Instant retiredAt) {

  // Transaction only
  public static LabelResponse fromEntity(Label label) {
    return new LabelResponse(
        label.getId(),
        label.getCopy().getId(),
        label.getBook().getId(),
        label.getLibrary().getName(),
        label.getBook().getTitle(),
        label.getCopyNumber(),
        label.getText(),
        label.getPrintStatus(),
        label.getPrintedAt(),
        label.getPrintedBy() == null ? null : label.getPrintedBy().getFullName(),
        label.getRetiredAt());
  }
}
