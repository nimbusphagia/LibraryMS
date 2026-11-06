package com.nimbusphagia.library.inventory.internal.util;

import com.nimbusphagia.library.inventory.internal.dto.label.LabelParts;
import java.util.ArrayList;
import java.util.List;

public final class LabelTextComposer {

  private LabelTextComposer() {}

  public static String compose(LabelParts parts, int copyNumber) {
    List<String> pieces = new ArrayList<>();

    List<String> head = new ArrayList<>();
    if (parts.categoryCode() != null) head.add(parts.categoryCode());
    if (parts.titleCode() != null) head.add(parts.titleCode());
    if (!head.isEmpty()) pieces.add(String.join("-", head));

    if (parts.deweyCode() != null) pieces.add(parts.deweyCode());
    if (parts.malagaCode() != null) pieces.add(parts.malagaCode());
    if (parts.collectionVolume() != null) pieces.add("v." + parts.collectionVolume());
    if (parts.seriesVolume() != null) pieces.add("t." + parts.seriesVolume());
    pieces.add("c." + copyNumber);

    return String.join(" / ", pieces);
  }
}
