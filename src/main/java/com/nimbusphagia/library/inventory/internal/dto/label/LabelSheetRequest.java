package com.nimbusphagia.library.inventory.internal.dto.label;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record LabelSheetRequest(
    @NotEmpty(message = "At least one copy is required") List<@NotNull Long> copyIds) {}
