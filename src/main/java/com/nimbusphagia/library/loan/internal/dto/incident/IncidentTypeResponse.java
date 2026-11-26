package com.nimbusphagia.library.loan.internal.dto.incident;

import com.nimbusphagia.library.loan.internal.entity.VisitorIncidentType;

public record IncidentTypeResponse(
    Long id, String name, int weight, boolean serious, boolean active) {

  public static IncidentTypeResponse fromEntity(VisitorIncidentType type) {
    return new IncidentTypeResponse(
        type.getId(), type.getName(), type.getWeight(), type.isSerious(), type.isActive());
  }
}
