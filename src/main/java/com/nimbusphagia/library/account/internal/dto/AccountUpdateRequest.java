package com.nimbusphagia.library.account.internal.dto;

import com.nimbusphagia.library.account.internal.entity.AccountRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountUpdateRequest(
    @NotBlank @Size(max = 255) String fullName, @NotNull AccountRole role, Long institutionId) {

  public AccountUpdateRequest {
    fullName = fullName == null ? null : fullName.strip();
  }
}
