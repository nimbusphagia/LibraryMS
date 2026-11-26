package com.nimbusphagia.library.loan.internal.controller;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.loan.internal.dto.incident.IncidentRequest;
import com.nimbusphagia.library.loan.internal.dto.incident.IncidentResponse;
import com.nimbusphagia.library.loan.internal.dto.incident.IncidentTypeRequest;
import com.nimbusphagia.library.loan.internal.dto.incident.IncidentTypeResponse;
import com.nimbusphagia.library.loan.internal.service.IncidentService;
import com.nimbusphagia.library.shared.security.LibrarianOnly;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/institutions/{institutionId}")
@RequiredArgsConstructor
class IncidentController {
  private final IncidentService incidentService;

  @GetMapping("/incident-types")
  @StaffOnly
  List<IncidentTypeResponse> findTypes(@PathVariable Long institutionId) {
    return incidentService.findTypes(institutionId);
  }

  @PostMapping("/incident-types")
  @LibrarianOnly
  @ResponseStatus(HttpStatus.CREATED)
  IncidentTypeResponse createType(
      @PathVariable Long institutionId, @Valid @RequestBody IncidentTypeRequest request) {
    return incidentService.createType(institutionId, request);
  }

  @PutMapping("/incident-types/{id}")
  @LibrarianOnly
  IncidentTypeResponse editType(
      @PathVariable Long institutionId,
      @PathVariable Long id,
      @Valid @RequestBody IncidentTypeRequest request) {
    return incidentService.editType(institutionId, id, request);
  }

  @GetMapping("/visitors/{visitorId}/incidents")
  @StaffOnly
  List<IncidentResponse> findByVisitor(
      @PathVariable Long institutionId, @PathVariable Long visitorId) {
    return incidentService.findByVisitor(institutionId, visitorId);
  }

  @PostMapping("/visitors/{visitorId}/incidents")
  @LibrarianOnly
  @ResponseStatus(HttpStatus.CREATED)
  IncidentResponse record(
      @PathVariable Long institutionId,
      @PathVariable Long visitorId,
      @Valid @RequestBody IncidentRequest request,
      @AuthenticationPrincipal AccountPrincipal principal) {
    return incidentService.record(institutionId, visitorId, request, principal.getId());
  }

  @DeleteMapping("/visitors/{visitorId}/incidents/{id}")
  @LibrarianOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(
      @PathVariable Long institutionId, @PathVariable Long visitorId, @PathVariable Long id) {
    incidentService.delete(institutionId, visitorId, id);
  }
}
