package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.inventory.internal.entity.LabelOrigin;
import java.util.List;

/** What a new label would get, part by part, under the library's scheme. */
public record LabelSuggestion(
    Long schemeId,
    List<PartSuggestion> parts,
    int copyNumber,
    String text,
    boolean collides,
    List<LabelPart> missing) {

  // value is what is used when the part is not sent: a reused manual value or the base
  public record PartSuggestion(
      LabelPart part,
      boolean required,
      boolean editable,
      String value,
      LabelOrigin origin,
      String base,
      List<String> alternatives) {}
}
