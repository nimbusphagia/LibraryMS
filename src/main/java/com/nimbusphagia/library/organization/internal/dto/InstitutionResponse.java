package com.nimbusphagia.library.organization.internal.dto;

import com.nimbusphagia.library.organization.internal.entity.Institution;

public record InstitutionResponse(Long id, String name, String timeZone, boolean active) {
  public static InstitutionResponse fromEntity(Institution institution) {
    return new InstitutionResponse(
        institution.getId(),
        institution.getName(),
        institution.getTimeZone(),
        institution.isActive());
  }
}
