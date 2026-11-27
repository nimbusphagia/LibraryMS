package com.nimbusphagia.library.inventory.internal.dto.scheme;

import jakarta.validation.constraints.NotNull;

public record LabelSchemeAssignRequest(
    @NotNull(message = "Label scheme id is missing") Long labelSchemeId) {}
