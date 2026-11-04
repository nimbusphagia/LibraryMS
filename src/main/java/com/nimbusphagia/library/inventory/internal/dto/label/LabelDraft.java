package com.nimbusphagia.library.inventory.internal.dto.label;

import com.nimbusphagia.library.inventory.internal.entity.NotationSource;

public record LabelDraft(
    String categoryCode,
    String collectionCode,
    String deweyCode,
    NotationSource malagaSource,
    String malagaCode,
    Integer collectionVolume,
    Integer seriesVolume,
    Integer copyNumber,
    String text) {}
