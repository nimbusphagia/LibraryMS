package com.nimbusphagia.library.catalog;

public record BookLabelData(
    Long bookId,
    String title,
    String categoryCode,
    String deweyCode,
    Integer collectionVolume,
    Integer seriesVolume) {}
