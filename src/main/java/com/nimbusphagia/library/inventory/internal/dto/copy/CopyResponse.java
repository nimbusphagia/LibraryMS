package com.nimbusphagia.library.inventory.internal.dto.copy;

import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.inventory.internal.dto.label.CopyLabelResponse;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;
import java.time.Instant;

public record CopyResponse(
    Long id,
    Long libraryId,
    Long bookId,
    String bookTitle,
    int copyNumber,
    Long shelfId,
    String shelfName,
    CopyStatus status,
    Instant damagedAt,
    String notes,
    CopyLabelResponse label) {

  public static CopyResponse of(Copy copy, BookLabelData book, CopyLabelResponse label) {
    return new CopyResponse(
        copy.getId(),
        copy.getLibrary().getId(),
        copy.getBook().getId(),
        book == null ? null : book.title(),
        copy.getIteration(),
        copy.getShelf().getId(),
        copy.getShelf().getName(),
        copy.getStatus(),
        copy.getDamagedAt(),
        copy.getNotes(),
        label);
  }
}
