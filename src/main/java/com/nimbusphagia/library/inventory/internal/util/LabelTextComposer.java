package com.nimbusphagia.library.inventory.internal.util;

import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.inventory.internal.entity.LabelLayout;
import com.nimbusphagia.library.inventory.internal.entity.LabelLayout.PartRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LabelTextComposer {

  private LabelTextComposer() {}

  // Lines joined with " / "; parts without a value and lines left empty are skipped. A null copy
  // number leaves it out, which gives the code a book's copies share.
  public static String compose(
      LabelLayout layout, Map<LabelPart, String> values, Integer copyNumber) {
    List<String> lines = new ArrayList<>();
    for (LabelLayout.Line line : layout.lines()) {
      List<String> pieces = new ArrayList<>();
      for (PartRule rule : line.parts()) {
        String value =
            rule.part() == LabelPart.COPY_NUMBER
                ? (copyNumber == null ? null : String.valueOf(copyNumber))
                : values.get(rule.part());
        if (value != null) {
          pieces.add(rule.prefix() == null ? value : rule.prefix() + value);
        }
      }
      if (!pieces.isEmpty()) {
        lines.add(String.join(line.separator(), pieces));
      }
    }
    return String.join(" / ", lines);
  }
}
