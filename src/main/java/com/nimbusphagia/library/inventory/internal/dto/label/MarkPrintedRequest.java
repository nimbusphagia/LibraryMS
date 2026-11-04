package com.nimbusphagia.library.inventory.internal.dto.label;

import jakarta.validation.constraints.NotEmpty;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record MarkPrintedRequest(
    @NotEmpty(message = "At least one label id is required") Set<Long> labelIds) {

  public MarkPrintedRequest {
    labelIds =
        labelIds == null
            ? null
            : labelIds.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
  }
}
