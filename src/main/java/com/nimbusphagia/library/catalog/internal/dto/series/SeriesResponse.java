package com.nimbusphagia.library.catalog.internal.dto.series;

import com.nimbusphagia.library.catalog.internal.entity.Series;

public record SeriesResponse(Long id, Long institutionId, String name) {
  public static SeriesResponse fromEntity(Series series) {
    return new SeriesResponse(series.getId(), series.getInstitutionId(), series.getName());
  }
}
