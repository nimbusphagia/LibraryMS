package com.nimbusphagia.library.inventory.internal.dto.copy;

import com.nimbusphagia.library.inventory.internal.entity.CopyConditionEvent;
import com.nimbusphagia.library.inventory.internal.entity.CopyConditionEventType;
import java.time.Instant;

public record CopyConditionEventResponse(
    Long id,
    CopyConditionEventType type,
    String reason,
    Long actorId,
    String actorName,
    Instant createdAt) {

  public static CopyConditionEventResponse fromEntity(CopyConditionEvent event) {
    return new CopyConditionEventResponse(
        event.getId(),
        event.getType(),
        event.getReason(),
        event.getActor().getId(),
        event.getActor().getFullName(),
        event.getCreatedAt());
  }
}
