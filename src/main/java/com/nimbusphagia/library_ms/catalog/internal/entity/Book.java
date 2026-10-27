package com.nimbusphagia.library_ms.catalog.internal.entity;

import com.nimbusphagia.library_ms.shared.model.BaseEntity;

/** Book */
public class Book extends BaseEntity {

  String publicId;
  String isbn;
  Author author;
  Publisher publisher;
  Integer year;
  String description;
}
