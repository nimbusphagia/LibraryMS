package com.nimbusphagia.library.loan.internal.dto.incident;

import com.nimbusphagia.library.loan.internal.entity.VisitorIncident;
import java.time.Instant;

public record IncidentResponse(
    Long id,
    IncidentTypeResponse type,
    Long loanLineId,
    Long recordedById,
    String recordedByName,
    String notes,
    Instant createdAt) {

  public static IncidentResponse fromEntity(VisitorIncident incident) {
    return new IncidentResponse(
        incident.getId(),
        IncidentTypeResponse.fromEntity(incident.getType()),
        incident.getLoanLine() == null ? null : incident.getLoanLine().getId(),
        incident.getRecordedBy().getId(),
        incident.getRecordedBy().getFullName(),
        incident.getNotes(),
        incident.getCreatedAt());
  }
}
