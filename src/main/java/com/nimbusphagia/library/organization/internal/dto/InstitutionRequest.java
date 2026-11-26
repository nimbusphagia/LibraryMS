package com.nimbusphagia.library.organization.internal.dto;

import com.nimbusphagia.library.organization.internal.entity.Institution;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstitutionRequest(
    @NotBlank(message = "Institution name is missing") @Size(max = 255) String name,
    @NotBlank(message = "Time zone is missing") @Size(max = 64) String timeZone) {

  public InstitutionRequest {
    name = name == null ? null : name.strip();
    timeZone = timeZone == null ? null : timeZone.strip();
  }

  public Institution toEntity(Long organizationId) {
    return Institution.builder()
        .organizationId(organizationId)
        .name(name)
        .timeZone(timeZone)
        .build();
  }

  public Institution replace(Institution institution) {
    institution.setName(name);
    institution.setTimeZone(timeZone);
    return institution;
  }
}
