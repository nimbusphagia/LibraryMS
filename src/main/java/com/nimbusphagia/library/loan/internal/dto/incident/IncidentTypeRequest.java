package com.nimbusphagia.library.loan.internal.dto.incident;

import com.nimbusphagia.library.loan.internal.entity.VisitorIncidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record IncidentTypeRequest(
    @NotBlank(message = "Incident type name is missing") @Size(max = 255) String name,
    @NotNull(message = "Weight is missing") @Positive Integer weight,
    boolean serious,
    Boolean active) {

  public IncidentTypeRequest {
    name = name == null ? null : name.strip();
    active = active == null || active;
  }

  public VisitorIncidentType replace(VisitorIncidentType type) {
    type.setName(name);
    type.setWeight(weight);
    type.setSerious(serious);
    type.setActive(active);
    return type;
  }
}
