package com.nimbusphagia.library.catalog;

public record BookLabelData(
    Long bookId,
    String title,
    String authorLastName,
    String publisherName,
    String categoryCode,
    String collectionCode,
    String deweyCode,
    Integer collectionVolume,
    Integer seriesVolume) {}
