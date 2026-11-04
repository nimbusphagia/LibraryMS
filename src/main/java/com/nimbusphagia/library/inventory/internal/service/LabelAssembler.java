package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelDraft;
import com.nimbusphagia.library.inventory.internal.util.MalagaResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LabelAssembler {

  public LabelDraft assemble(BookLabelData book, int copyNumber, MalagaResult malaga) {
    String dewey = clean(book.deweyCode());
    if (dewey == null) {
      throw new BadRequestException(
          "Book \"" + book.title() + "\" needs a Dewey classification to get a label");
    }
    String category = clean(book.categoryCode());
    String collection = clean(book.collectionCode());

    String text =
        buildText(
            category,
            collection,
            dewey,
            malaga.code(),
            book.collectionVolume(),
            book.seriesVolume(),
            copyNumber);

    return new LabelDraft(
        category,
        collection,
        dewey,
        malaga.source(),
        malaga.code(),
        book.collectionVolume(),
        book.seriesVolume(),
        copyNumber,
        text);
  }

  // category-collection / dewey / malaga / v.N / t.N / c.N, null pieces skipped
  private static String buildText(
      String category,
      String collection,
      String dewey,
      String malagaCode,
      Integer collectionVolume,
      Integer seriesVolume,
      int copyNumber) {
    List<String> pieces = new ArrayList<>();

    List<String> head = new ArrayList<>();
    if (category != null) head.add(category);
    if (collection != null) head.add(collection);
    if (!head.isEmpty()) pieces.add(String.join("-", head));

    pieces.add(dewey);
    pieces.add(malagaCode);
    if (collectionVolume != null) pieces.add("v." + collectionVolume);
    if (seriesVolume != null) pieces.add("t." + seriesVolume);
    pieces.add("c." + copyNumber);

    return String.join(" / ", pieces);
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }
}
