package com.nimbusphagia.library.loan.internal.service;

import com.nimbusphagia.library.exception.ErrorCodes;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.loan.internal.dto.visitor.VisitorFilter;
import com.nimbusphagia.library.loan.internal.dto.visitor.VisitorRequest;
import com.nimbusphagia.library.loan.internal.dto.visitor.VisitorResponse;
import com.nimbusphagia.library.loan.internal.entity.SchoolGrade;
import com.nimbusphagia.library.loan.internal.entity.Visitor;
import com.nimbusphagia.library.loan.internal.entity.VisitorRole;
import com.nimbusphagia.library.loan.internal.repository.LoanMovementLineRepository;
import com.nimbusphagia.library.loan.internal.repository.SchoolGradeRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorRoleRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorSpecifications;
import com.nimbusphagia.library.organization.AccessGuard;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VisitorService {
  private final VisitorRepository visitorRepository;
  private final VisitorRoleRepository roleRepository;
  private final SchoolGradeRepository gradeRepository;
  private final LoanMovementLineRepository lineRepository;
  private final AccessGuard accessGuard;

  @Transactional
  public VisitorResponse create(Long institutionId, VisitorRequest request) {
    VisitorRole role = findRole(institutionId, request.roleId());
    SchoolGrade grade = resolveGrade(institutionId, role, request.gradeId());
    assertNameAvailable(institutionId, request.name(), grade, null);

    Visitor visitor =
        visitorRepository.save(
            Visitor.builder()
                .institutionId(institutionId)
                .name(request.name())
                .role(role)
                .grade(grade)
                .build());
    return VisitorResponse.fromEntity(visitor);
  }

  @Transactional
  public VisitorResponse edit(Long institutionId, Long id, VisitorRequest request) {
    Visitor visitor = find(institutionId, id);
    VisitorRole role = findRole(institutionId, request.roleId());
    SchoolGrade grade = resolveGrade(institutionId, role, request.gradeId());
    if (Boolean.TRUE.equals(visitor.getActive())) {
      assertNameAvailable(institutionId, request.name(), grade, id);
    }

    visitor.setName(request.name());
    visitor.setRole(role);
    visitor.setGrade(grade);
    return VisitorResponse.fromEntity(visitor);
  }

  @Transactional(readOnly = true)
  public Page<VisitorResponse> findAll(Long institutionId, VisitorFilter filter, Pageable pageable) {
    return visitorRepository
        .findAll(VisitorSpecifications.from(institutionId, filter), pageable)
        .map(VisitorResponse::fromEntity);
  }

  @Transactional(readOnly = true)
  public VisitorResponse findOne(Long institutionId, Long id) {
    return VisitorResponse.fromEntity(find(institutionId, id));
  }

  @Transactional
  public VisitorResponse deactivate(Long institutionId, Long id) {
    Visitor visitor = find(institutionId, id);
    if (lineRepository.existsByMovementLoanedToIdAndResolvedAtIsNull(id)) {
      throw new ConflictException(
          ErrorCodes.VISITOR_HAS_OPEN_LOANS,
          "This visitor still has copies on loan; resolve them before deactivating");
    }
    visitor.setActive(false);
    return VisitorResponse.fromEntity(visitor);
  }

  @Transactional
  public VisitorResponse activate(Long institutionId, Long id) {
    Visitor visitor = find(institutionId, id);
    assertNameAvailable(institutionId, visitor.getName(), visitor.getGrade(), id);
    visitor.setActive(true);
    return VisitorResponse.fromEntity(visitor);
  }

  // Helpers
  private Visitor find(Long institutionId, Long id) {
    return visitorRepository
        .findByIdAndInstitutionId(id, institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Visitor not found"));
  }

  private VisitorRole findRole(Long institutionId, Long roleId) {
    return roleRepository
        .findByIdAndOrganizationIdAndInstitutionId(roleId, organizationId(), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Visitor role not found"));
  }

  private SchoolGrade resolveGrade(Long institutionId, VisitorRole role, Long gradeId) {
    if (!role.isRequiresGrade()) {
      if (gradeId != null) {
        throw new BadRequestException("Visitors with this role do not have a school grade");
      }
      return null;
    }
    if (gradeId == null) {
      throw new BadRequestException("Visitors with this role must have a school grade");
    }
    return gradeRepository
        .findByIdAndOrganizationIdAndInstitutionId(gradeId, organizationId(), institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("School grade not found"));
  }

  private void assertNameAvailable(
      Long institutionId, String name, SchoolGrade grade, Long selfId) {
    Optional<Visitor> clash =
        grade == null
            ? visitorRepository.findFirstByInstitutionIdAndGradeIsNullAndNameIgnoreCaseAndActiveTrue(
                institutionId, name)
            : visitorRepository.findFirstByInstitutionIdAndGradeIdAndNameIgnoreCaseAndActiveTrue(
                institutionId, grade.getId(), name);

    if (clash.isPresent() && !clash.get().getId().equals(selfId)) {
      throw new ConflictException(
          (grade == null
                  ? "Another active visitor without a grade is already named \""
                  : "Another active student in this grade is already named \"")
              + name
              + "\"; rename one of them");
    }
  }

  private Long organizationId() {
    return accessGuard.current().getOrganizationId();
  }
}
