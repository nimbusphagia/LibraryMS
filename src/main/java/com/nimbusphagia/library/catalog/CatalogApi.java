package com.nimbusphagia.library.catalog;

import java.util.Map;
import java.util.Set;

public interface CatalogApi {

  BookLabelData getBookLabelData(Long bookId);

  // Batch version. Ids that don't exist are absent from the map.
  Map<Long, BookLabelData> getBookLabelData(Set<Long> bookIds);
}
