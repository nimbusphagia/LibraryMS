package com.nimbusphagia.library.inventory.internal.util;

import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.inventory.internal.entity.NotationSource;
import java.text.Normalizer;
import org.springframework.stereotype.Component;

/** TEMPORARY. Delete this class when the real MalagaResolver is written. */
@Component
public class StubMalagaResolver implements MalagaResolver {

  @Override
  public MalagaResult resolve(Long libraryId, BookLabelData book) {
    if (hasText(book.authorLastName())) {
      return new MalagaResult(NotationSource.AUTHOR, placeholder(book.authorLastName()));
    }
    if (hasText(book.publisherName())) {
      return new MalagaResult(NotationSource.PUBLISHER, placeholder(book.publisherName()));
    }
    throw new BadRequestException(
        "Book \"" + book.title() + "\" needs an author last name or a publisher to get a label");
  }

  private static boolean hasText(String s) {
    return s != null && !s.isBlank();
  }

  // STUB: first 3 letters/digits, uppercase, accents removed. Not the real notation.
  private static String placeholder(String source) {
    String normalized = Normalizer.normalize(source.strip(), Normalizer.Form.NFD);
    StringBuilder sb = new StringBuilder();
    for (char c : normalized.toCharArray()) {
      if (Character.isLetterOrDigit(c) && sb.length() < 3) {
        sb.append(Character.toUpperCase(c));
      }
    }
    return sb.length() == 0 ? "X" : sb.toString();
  }
}
