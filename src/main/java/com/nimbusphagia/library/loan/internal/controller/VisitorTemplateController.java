package com.nimbusphagia.library.loan.internal.controller;

import com.nimbusphagia.library.loan.internal.dto.schoolGrade.SchoolGradeRequest;
import com.nimbusphagia.library.loan.internal.dto.schoolGrade.SchoolGradeResponse;
import com.nimbusphagia.library.loan.internal.dto.visitorRole.VisitorRoleRequest;
import com.nimbusphagia.library.loan.internal.dto.visitorRole.VisitorRoleResponse;
import com.nimbusphagia.library.loan.internal.service.VisitorCatalogService;
import com.nimbusphagia.library.shared.security.AdminOnly;
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

// The organization's templates, copied into each institution when it is created
@RestController
@RequestMapping("/api/templates")
@AdminOnly
@RequiredArgsConstructor
class VisitorTemplateController {
  private final VisitorCatalogService catalogService;

  @GetMapping("/visitor-roles")
  List<VisitorRoleResponse> findRoles() {
    return catalogService.findRoles(null);
  }

  @PostMapping("/visitor-roles")
  @ResponseStatus(HttpStatus.CREATED)
  VisitorRoleResponse createRole(@Valid @RequestBody VisitorRoleRequest request) {
    return catalogService.createRole(null, request);
  }

  @PutMapping("/visitor-roles/{id}")
  VisitorRoleResponse editRole(
      @PathVariable Long id, @Valid @RequestBody VisitorRoleRequest request) {
    return catalogService.editRole(null, id, request);
  }

  @DeleteMapping("/visitor-roles/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteRole(@PathVariable Long id) {
    catalogService.deleteRole(null, id);
  }

  @GetMapping("/school-grades")
  List<SchoolGradeResponse> findGrades() {
    return catalogService.findGrades(null);
  }

  @PostMapping("/school-grades")
  @ResponseStatus(HttpStatus.CREATED)
  SchoolGradeResponse createGrade(@Valid @RequestBody SchoolGradeRequest request) {
    return catalogService.createGrade(null, request);
  }

  @DeleteMapping("/school-grades/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteGrade(@PathVariable Long id) {
    catalogService.deleteGrade(null, id);
  }
}
