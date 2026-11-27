package com.nimbusphagia.library.catalog;

public record BookLabelData(
    Long bookId,
    String title,
    Long authorId,
    String authorFirstName,
    String authorLastName,
    String categoryCode,
    String subcategoryCode,
    String deweyNumber,
    Integer collectionVolume,
    Integer seriesVolume) {}
