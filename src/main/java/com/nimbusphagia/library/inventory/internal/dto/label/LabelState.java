package com.nimbusphagia.library.inventory.internal.dto.label;

// Derived from what is stuck on the copy and what its book's pattern gives for its number
public enum LabelState {
  // The book has no label pattern in the library yet
  NO_PATTERN,
  UNLABELLED,
  LABELLED,
  // The stuck label no longer matches; placedText tells which one to replace
  OUTDATED
}
