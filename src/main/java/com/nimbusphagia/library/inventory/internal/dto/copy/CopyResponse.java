package com.nimbusphagia.library.inventory.internal.dto.copy;

import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;
import com.nimbusphagia.library.inventory.internal.entity.Label;
import com.nimbusphagia.library.inventory.internal.entity.PrintStatus;

public record CopyResponse(
    Long id,
    Long libraryId,
    Long bookId,
    String bookTitle,
    int copyNumber,
    Long shelfId,
    String shelfName,
    CopyStatus status,
    String notes,
    Long labelId,
    String labelText,
    PrintStatus labelPrintStatus) {

  public static CopyResponse of(Copy copy, BookLabelData book, Label label) {
    return new CopyResponse(
        copy.getId(),
        copy.getLibrary().getId(),
        copy.getBook().getId(),
        book == null ? null : book.title(),
        copy.getIteration(),
        copy.getShelf().getId(),
        copy.getShelf().getName(),
        copy.getStatus(),
        copy.getNotes(),
        label == null ? null : label.getId(),
        label == null ? null : label.getText(),
        label == null ? null : label.getPrintStatus());
  }
}
