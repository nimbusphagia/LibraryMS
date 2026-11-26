package com.nimbusphagia.library.account.internal.dto;

import com.nimbusphagia.library.account.internal.entity.AccountRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountCreateRequest(
    @NotBlank @Size(max = 100) String username,
    @NotBlank @Size(max = 255) String fullName,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotNull AccountRole role,
    Long institutionId) {

  public AccountCreateRequest {
    username = username == null ? null : username.strip();
    fullName = fullName == null ? null : fullName.strip();
  }
}
