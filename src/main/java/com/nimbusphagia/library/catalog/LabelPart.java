package com.nimbusphagia.library.catalog;

/**
 * The closed set of pieces a label is built from. All but AUTHOR_MALAGA, which the librarian
 * chooses, and COPY_NUMBER, which belongs to the copy, come from catalog data.
 */
public enum LabelPart {
  CATEGORY,
  SUBCATEGORY,
  DEWEY,
  AUTHOR_LETTERS,
  AUTHOR_MALAGA,
  TITLE,
  COLLECTION_VOLUME,
  SERIES_VOLUME,
  COPY_NUMBER
}
