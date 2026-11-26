package com.nimbusphagia.library.organization.internal.controller;

import com.nimbusphagia.library.organization.internal.dto.InstitutionRequest;
import com.nimbusphagia.library.organization.internal.dto.InstitutionResponse;
import com.nimbusphagia.library.organization.internal.service.InstitutionService;
import com.nimbusphagia.library.shared.security.AdminOnly;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/institutions")
@RequiredArgsConstructor
class InstitutionController {
  private final InstitutionService institutionService;

  @GetMapping
  @StaffOnly
  List<InstitutionResponse> findAll(@RequestParam(required = false) Boolean active) {
    return institutionService.findAll(active);
  }

  @GetMapping("/{institutionId}")
  @StaffOnly
  InstitutionResponse findOne(@PathVariable Long institutionId) {
    return institutionService.findOne(institutionId);
  }

  @PostMapping
  @AdminOnly
  @ResponseStatus(HttpStatus.CREATED)
  InstitutionResponse create(@Valid @RequestBody InstitutionRequest request) {
    return institutionService.create(request);
  }

  @PutMapping("/{institutionId}")
  @AdminOnly
  InstitutionResponse edit(
      @PathVariable Long institutionId, @Valid @RequestBody InstitutionRequest request) {
    return institutionService.edit(institutionId, request);
  }

  @PatchMapping("/{institutionId}/deactivate")
  @AdminOnly
  InstitutionResponse deactivate(@PathVariable Long institutionId) {
    return institutionService.deactivate(institutionId);
  }

  @PatchMapping("/{institutionId}/activate")
  @AdminOnly
  InstitutionResponse activate(@PathVariable Long institutionId) {
    return institutionService.activate(institutionId);
  }
}
