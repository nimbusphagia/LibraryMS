package com.nimbusphagia.library.inventory.internal.dto.label;

/**
 * What a change does to labels: book patterns that took it, copies whose stuck label it leaves
 * outdated, and patterns that could not follow it (a missing part or a taken code) and wait a
 * refresh.
 */
public record LabelImpact(int changed, int outdated, int unresolved) {

  public static final LabelImpact NONE = new LabelImpact(0, 0, 0);

  public LabelImpact plus(LabelImpact other) {
    return new LabelImpact(
        changed + other.changed, outdated + other.outdated, unresolved + other.unresolved);
  }
}
