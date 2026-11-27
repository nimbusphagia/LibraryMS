package com.nimbusphagia.library.inventory.internal.dto.label;

// Labels a change updated by themselves, and labels it left OUTDATED
public record LabelImpact(int updated, int outdated) {}
