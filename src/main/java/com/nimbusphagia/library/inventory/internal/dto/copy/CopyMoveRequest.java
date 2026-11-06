package com.nimbusphagia.library.inventory.internal.dto.copy;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CopyMoveRequest(
    @NotNull(message = "Shelf id is missing") Long shelfId,
    @NotEmpty(message = "At least one copy is required")
        @Size(max = 100, message = "At most 100 copies can be moved at once")
        List<@NotNull(message = "Copy id is missing") Long> copyIds) {}
