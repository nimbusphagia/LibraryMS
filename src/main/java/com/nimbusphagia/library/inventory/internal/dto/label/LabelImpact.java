package com.nimbusphagia.library.inventory.internal.dto.label;

/**
 * What a change does to labels: pending ones updated by themselves, printed ones left OUTDATED,
 * and book patterns that could not follow it (a missing part or a taken code) and wait a refresh.
 */
public record LabelImpact(int updated, int outdated, int unresolved) {

  public static final LabelImpact NONE = new LabelImpact(0, 0, 0);

  public LabelImpact plus(LabelImpact other) {
    return new LabelImpact(
        updated + other.updated, outdated + other.outdated, unresolved + other.unresolved);
  }
}
