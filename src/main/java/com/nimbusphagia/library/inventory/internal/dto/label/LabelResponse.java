package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.inventory.internal.entity.Label;
import com.nimbusphagia.library.inventory.internal.entity.PrintStatus;
import java.time.Instant;

public record LabelResponse(
    String libraryName,
    String bookName,
    String categoryCode,
    String collectionCode,
    String deweyCode,
    String malagaCode,
    Integer collectionVolume,
    Integer seriesVolume,
    Integer copyIteration,
    String text,
    boolean customized,
    PrintStatus printStatus,
    Instant printedAt,
    String printedBy) {
  public static LabelResponse fromEntity(Label label) {
    // Should read all info from copy's book and append the codes
    // The only calculated code should be the malagaCode, but that needs a class or helper method
    return new LabelResponse();
  }
}
