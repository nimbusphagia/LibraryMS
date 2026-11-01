package com.nimbusphagia.library_ms.catalog.internal.dto.series;

import com.nimbusphagia.library_ms.catalog.internal.entity.Author;
import com.nimbusphagia.library_ms.catalog.internal.entity.Series;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SeriesRequest(
    @NotBlank(message = "Series name is missing") @Size(max = 255) String name) {

  public SeriesRequest {
    name = name == null ? null : name.strip();
  }

  public Series toEntity(Author author) {
    return Series.builder().name(name).author(author).build();
  }

  public Series replace(Series series) {
    series.setName(name);
    return series;
  }
}
