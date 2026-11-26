package com.nimbusphagia.library.loan.internal.service;

import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.loan.internal.dto.schoolGrade.SchoolGradeRequest;
import com.nimbusphagia.library.loan.internal.dto.schoolGrade.SchoolGradeResponse;
import com.nimbusphagia.library.loan.internal.dto.visitorRole.VisitorRoleRequest;
import com.nimbusphagia.library.loan.internal.dto.visitorRole.VisitorRoleResponse;
import com.nimbusphagia.library.loan.internal.entity.GradeLevel;
import com.nimbusphagia.library.loan.internal.entity.SchoolGrade;
import com.nimbusphagia.library.loan.internal.entity.VisitorRole;
import com.nimbusphagia.library.loan.internal.repository.SchoolGradeRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorRoleRepository;
import com.nimbusphagia.library.organization.AccessGuard;
import com.nimbusphagia.library.organization.InstitutionCreatedEvent;
import com.nimbusphagia.library.organization.OrganizationCreatedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Visitor roles and school grades of an institution, or the organization's templates when the
 * institution id is null.
 */
@Service
@RequiredArgsConstructor
public class VisitorCatalogService {
  private static final List<VisitorRoleRequest> DEFAULT_ROLES =
      List.of(
          new VisitorRoleRequest("Student", true, 7, 5, 1, 0),
          new VisitorRoleRequest("Teacher", false, 10, 5, 1, 0));

  private final VisitorRoleRepository roleRepository;
  private final SchoolGradeRepository gradeRepository;
  private final VisitorRepository visitorRepository;
  private final AccessGuard accessGuard;

  @Transactional(readOnly = true)
  public List<VisitorRoleResponse> findRoles(Long institutionId) {
    return roleRepository
        .findByOrganizationIdAndInstitutionIdOrderByIdAsc(organizationId(), institutionId)
        .stream()
        .map(VisitorRoleResponse::fromEntity)
        .toList();
  }

  @Transactional
  public VisitorRoleResponse createRole(Long institutionId, VisitorRoleRequest request) {
    Long organizationId = organizationId();
    if (roleRepository.existsByOrganizationIdAndInstitutionIdAndNameIgnoreCase(
        organizationId, institutionId, request.name())) {
      throw new ConflictException("Visitor role name already exists");
    }
    return VisitorRoleResponse.fromEntity(
        roleRepository.save(request.toEntity(organizationId, institutionId)));
  }

  @Transactional
  public VisitorRoleResponse editRole(Long institutionId, Long id, VisitorRoleRequest request) {
    VisitorRole role = findRole(institutionId, id);
    if (roleRepository.existsByOrganizationIdAndInstitutionIdAndNameIgnoreCaseAndIdNot(
        role.getOrganizationId(), institutionId, request.name(), id)) {
      throw new ConflictException("Visitor role name already exists");
    }
    if (role.isRequiresGrade() != request.requiresGrade() && visitorRepository.existsByRoleId(id)) {
      throw new ConflictException(
          "Visitors already have this role; create a new role to change whether it needs a grade");
    }
    return VisitorRoleResponse.fromEntity(request.replace(role));
  }

  @Transactional
  public void deleteRole(Long institutionId, Long id) {
    VisitorRole role = findRole(institutionId, id);
    if (visitorRepository.existsByRoleId(id)) {
      throw new ConflictException("Visitors have this role, so it cannot be deleted");
    }
    roleRepository.delete(role);
  }

  @Transactional(readOnly = true)
  public List<SchoolGradeResponse> findGrades(Long institutionId) {
    return gradeRepository
        .findByOrganizationIdAndInstitutionIdOrderByLevelAscGradeAsc(
            organizationId(), institutionId)
        .stream()
        .map(SchoolGradeResponse::fromEntity)
        .toList();
  }

  @Transactional
  public SchoolGradeResponse createGrade(Long institutionId, SchoolGradeRequest request) {
    if (request.grade() > request.level().lastGrade()) {
      throw new BadRequestException(
          request.level() + " grades go from 1 to " + request.level().lastGrade());
    }
    Long organizationId = organizationId();
    if (gradeRepository.existsByOrganizationIdAndInstitutionIdAndLevelAndGrade(
        organizationId, institutionId, request.level(), request.grade())) {
      throw new ConflictException("School grade already exists");
    }
    return SchoolGradeResponse.fromEntity(
        gradeRepository.save(
            SchoolGrade.builder()
                .organizationId(organizationId)
                .institutionId(institutionId)
                .level(request.level())
                .grade(request.grade())
                .build()));
  }

  @Transactional
  public void deleteGrade(Long institutionId, Long id) {
    SchoolGrade grade =
        gradeRepository
            .findByIdAndOrganizationIdAndInstitutionId(id, organizationId(), institutionId)
            .orElseThrow(() -> new ResourceNotFoundException("School grade not found"));
    if (visitorRepository.existsByGradeId(id)) {
      throw new ConflictException("Visitors are in this grade, so it cannot be deleted");
    }
    gradeRepository.delete(grade);
  }

  @EventListener
  void createDefaultTemplates(OrganizationCreatedEvent event) {
    Long organizationId = event.organizationId();
    roleRepository.saveAll(
        DEFAULT_ROLES.stream().map(r -> r.toEntity(organizationId, null)).toList());

    List<SchoolGrade> grades = new ArrayList<>();
    for (GradeLevel level : GradeLevel.values()) {
      IntStream.rangeClosed(1, level.lastGrade())
          .forEach(
              g ->
                  grades.add(
                      SchoolGrade.builder()
                          .organizationId(organizationId)
                          .level(level)
                          .grade(g)
                          .build()));
    }
    gradeRepository.saveAll(grades);
  }

  @EventListener
  void copyTemplates(InstitutionCreatedEvent event) {
    Long organizationId = event.organizationId();
    Long institutionId = event.institutionId();
    roleRepository.saveAll(
        roleRepository.findByOrganizationIdAndInstitutionIdOrderByIdAsc(organizationId, null).stream()
            .map(r -> r.toBuilder().institutionId(institutionId).build())
            .toList());
    gradeRepository.saveAll(
        gradeRepository
            .findByOrganizationIdAndInstitutionIdOrderByLevelAscGradeAsc(organizationId, null)
            .stream()
            .map(g -> g.toBuilder().institutionId(institutionId).build())
            .toList());
  }

  // Helpers
  private Long organizationId() {
    return accessGuard.current().getOrganizationId();
  }

  private VisitorRole findRole(Long institutionId, Long id) {
    return roleRepository
        .findByIdAndOrganizationIdAndInstitutionId(id, organizationId(), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Visitor role not found"));
  }
}
