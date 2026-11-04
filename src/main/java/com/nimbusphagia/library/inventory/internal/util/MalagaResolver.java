package com.nimbusphagia.library.inventory.internal.util;

import com.nimbusphagia.library.catalog.BookLabelData;

public interface MalagaResolver {

  /**
   * Picks the notation source (author last name, else publisher) and computes the code. libraryId
   * is there for the homonym lookup the real implementation will need.
   *
   * @throws com.nimbusphagia.library.exception.custom.BadRequestException if the book has neither
   *     an author last name nor a publisher
   */
  MalagaResult resolve(Long libraryId, BookLabelData book);
}
