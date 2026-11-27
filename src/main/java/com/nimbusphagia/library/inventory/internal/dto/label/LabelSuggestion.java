package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.inventory.internal.entity.LabelOrigin;
import java.util.List;

/**
 * What the book's next copy would print, part by part. A book with a pattern in the library
 * (existing) shows it; otherwise the values a new pattern would take under the library's scheme.
 */
public record LabelSuggestion(
    Long schemeId,
    boolean existing,
    List<PartSuggestion> parts,
    int copyNumber,
    String text,
    boolean collides,
    List<LabelPart> missing) {

  // value is what is used when the part is not sent: the pattern's, else the base
  public record PartSuggestion(
      LabelPart part,
      boolean required,
      boolean editable,
      String value,
      LabelOrigin origin,
      String base,
      List<String> alternatives,
      List<MalagaOption> malaga) {}

  // Only suggestions: the librarian picks, shortens or writes the code
  public record MalagaOption(String code, MalagaSource source, String entry, String qualifier) {}

  public enum MalagaSource {
    // Codes on other labels of the same author in this library
    AUTHOR_LABELS,
    TABLE
  }
}
