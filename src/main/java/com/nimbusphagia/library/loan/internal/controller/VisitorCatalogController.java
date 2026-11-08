package com.nimbusphagia.library.loan.internal.controller;

import com.nimbusphagia.library.loan.internal.dto.schoolGrade.SchoolGradeResponse;
import com.nimbusphagia.library.loan.internal.dto.visitorRole.VisitorRoleResponse;
import com.nimbusphagia.library.loan.internal.service.VisitorCatalogService;
import com.nimbusphagia.library.shared.security.StaffOnly;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@StaffOnly
@RequiredArgsConstructor
class VisitorCatalogController {
  private final VisitorCatalogService catalogService;

  @GetMapping("/visitor-roles")
  List<VisitorRoleResponse> findRoles() {
    return catalogService.findRoles();
  }

  @GetMapping("/school-grades")
  List<SchoolGradeResponse> findGrades() {
    return catalogService.findGrades();
  }
}
