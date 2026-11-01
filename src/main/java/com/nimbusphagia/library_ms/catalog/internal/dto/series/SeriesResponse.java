package com.nimbusphagia.library_ms.catalog.internal.dto.series;

import com.nimbusphagia.library_ms.catalog.internal.entity.Series;

public record SeriesResponse(Long id, String name) {
  public static SeriesResponse fromEntity(Series series) {
    return new SeriesResponse(series.getId(), series.getName());
  }
}
