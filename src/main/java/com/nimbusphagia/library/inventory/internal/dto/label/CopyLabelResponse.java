package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.inventory.internal.entity.BookLabel;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import java.time.Instant;

// text is what the copy should show; placedText what it shows
public record CopyLabelResponse(
    Long copyId,
    Long bookId,
    int copyNumber,
    LabelState state,
    String text,
    String placedText,
    Instant placedAt,
    String placedBy) {

  // Transaction only
  public static CopyLabelResponse of(Copy copy, BookLabel pattern) {
    String text = pattern == null ? null : pattern.textFor(copy.getIteration());
    LabelState state;
    if (pattern == null) {
      state = LabelState.NO_PATTERN;
    } else if (copy.getPlacedText() == null) {
      state = LabelState.UNLABELLED;
    } else if (copy.getPlacedText().equals(text)) {
      state = LabelState.LABELLED;
    } else {
      state = LabelState.OUTDATED;
    }
    return new CopyLabelResponse(
        copy.getId(),
        copy.getBook().getId(),
        copy.getIteration(),
        state,
        text,
        copy.getPlacedText(),
        copy.getPlacedAt(),
        copy.getPlacedBy() == null ? null : copy.getPlacedBy().getFullName());
  }
}
