package com.nimbusphagia.library.loan.internal.controller;

import com.nimbusphagia.library.loan.internal.dto.schoolGrade.SchoolGradeRequest;
import com.nimbusphagia.library.loan.internal.dto.schoolGrade.SchoolGradeResponse;
import com.nimbusphagia.library.loan.internal.dto.visitorRole.VisitorRoleRequest;
import com.nimbusphagia.library.loan.internal.dto.visitorRole.VisitorRoleResponse;
import com.nimbusphagia.library.loan.internal.service.VisitorCatalogService;
import com.nimbusphagia.library.shared.security.LibrarianOnly;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
class VisitorCatalogController {
  private final VisitorCatalogService catalogService;

  @GetMapping("/visitor-roles")
  @StaffOnly
  List<VisitorRoleResponse> findRoles(@PathVariable Long institutionId) {
    return catalogService.findRoles(institutionId);
  }

  @PostMapping("/visitor-roles")
  @LibrarianOnly
  @ResponseStatus(HttpStatus.CREATED)
  VisitorRoleResponse createRole(
      @PathVariable Long institutionId, @Valid @RequestBody VisitorRoleRequest request) {
    return catalogService.createRole(institutionId, request);
  }

  @PutMapping("/visitor-roles/{id}")
  @LibrarianOnly
  VisitorRoleResponse editRole(
      @PathVariable Long institutionId,
      @PathVariable Long id,
      @Valid @RequestBody VisitorRoleRequest request) {
    return catalogService.editRole(institutionId, id, request);
  }

  @DeleteMapping("/visitor-roles/{id}")
  @LibrarianOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteRole(@PathVariable Long institutionId, @PathVariable Long id) {
    catalogService.deleteRole(institutionId, id);
  }

  @GetMapping("/school-grades")
  @StaffOnly
  List<SchoolGradeResponse> findGrades(@PathVariable Long institutionId) {
    return catalogService.findGrades(institutionId);
  }

  @PostMapping("/school-grades")
  @LibrarianOnly
  @ResponseStatus(HttpStatus.CREATED)
  SchoolGradeResponse createGrade(
      @PathVariable Long institutionId, @Valid @RequestBody SchoolGradeRequest request) {
    return catalogService.createGrade(institutionId, request);
  }

  @DeleteMapping("/school-grades/{id}")
  @LibrarianOnly
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteGrade(@PathVariable Long institutionId, @PathVariable Long id) {
    catalogService.deleteGrade(institutionId, id);
  }
}
