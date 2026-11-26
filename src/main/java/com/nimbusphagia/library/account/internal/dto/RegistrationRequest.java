package com.nimbusphagia.library.account.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
    @NotBlank @Size(max = 255) String organizationName,
    @NotBlank @Size(max = 100) String username,
    @NotBlank @Size(max = 255) String fullName,
    @NotBlank @Size(min = 8, max = 72) String password) {

  public RegistrationRequest {
    organizationName = organizationName == null ? null : organizationName.strip();
    username = username == null ? null : username.strip();
    fullName = fullName == null ? null : fullName.strip();
  }
}
