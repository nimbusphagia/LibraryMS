package com.nimbusphagia.library.catalog;

public record BookLabelData(
    Long bookId,
    String title,
    String categoryCode,
    String subcategoryCode,
    String deweyNumber,
    Integer collectionVolume,
    Integer seriesVolume) {}
