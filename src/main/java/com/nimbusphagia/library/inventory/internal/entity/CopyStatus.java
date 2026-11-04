package com.nimbusphagia.library.inventory.internal.entity;

import java.util.EnumSet;
import java.util.Set;

public enum CopyStatus {
  AVAILABLE(true),
  LOST(true),
  DISCARDED(false),
  TRANSFERRED(false);

  private final boolean holdsNumber;

  CopyStatus(boolean holdsNumber) {
    this.holdsNumber = holdsNumber;
  }

  public boolean holdsNumber() {
    return holdsNumber;
  }

  public static Set<CopyStatus> numberHolding() {
    Set<CopyStatus> set = EnumSet.noneOf(CopyStatus.class);
    for (CopyStatus s : values()) {
      if (s.holdsNumber) set.add(s);
    }
    return set;
  }
}
