package com.nimbusphagia.library.loan.internal.service;

import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.loan.internal.dto.visitor.VisitorFilter;
import com.nimbusphagia.library.loan.internal.dto.visitor.VisitorRequest;
import com.nimbusphagia.library.loan.internal.dto.visitor.VisitorResponse;
import com.nimbusphagia.library.loan.internal.entity.SchoolGrade;
import com.nimbusphagia.library.loan.internal.entity.Visitor;
import com.nimbusphagia.library.loan.internal.entity.VisitorRole;
import com.nimbusphagia.library.loan.internal.repository.SchoolGradeRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorRoleRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorSpecifications;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VisitorService {
  private static final String STUDENT_ROLE_CODE = "STUDENT";

  private final VisitorRepository visitorRepository;
  private final VisitorRoleRepository roleRepository;
  private final SchoolGradeRepository gradeRepository;

  @Transactional
  public VisitorResponse create(VisitorRequest request) {
    VisitorRole role = findRole(request.roleId());
    SchoolGrade grade = resolveGrade(role, request.gradeId());
    assertNameAvailable(request.name(), grade, null);

    Visitor visitor =
        visitorRepository.save(
            Visitor.builder().name(request.name()).role(role).grade(grade).build());
    return VisitorResponse.fromEntity(visitor);
  }

  @Transactional
  public VisitorResponse edit(Long id, VisitorRequest request) {
    Visitor visitor = find(id);
    VisitorRole role = findRole(request.roleId());
    SchoolGrade grade = resolveGrade(role, request.gradeId());
    if (Boolean.TRUE.equals(visitor.getActive())) {
      assertNameAvailable(request.name(), grade, id);
    }

    visitor.setName(request.name());
    visitor.setRole(role);
    visitor.setGrade(grade);
    return VisitorResponse.fromEntity(visitor);
  }

  @Transactional(readOnly = true)
  public Page<VisitorResponse> findAll(VisitorFilter filter, Pageable pageable) {
    return visitorRepository
        .findAll(VisitorSpecifications.from(filter), pageable)
        .map(VisitorResponse::fromEntity);
  }

  @Transactional(readOnly = true)
  public VisitorResponse findOne(Long id) {
    return VisitorResponse.fromEntity(find(id));
  }

  @Transactional
  public VisitorResponse deactivate(Long id) {
    Visitor visitor = find(id);
    visitor.setActive(false);
    return VisitorResponse.fromEntity(visitor);
  }

  @Transactional
  public VisitorResponse activate(Long id) {
    Visitor visitor = find(id);
    assertNameAvailable(visitor.getName(), visitor.getGrade(), id);
    visitor.setActive(true);
    return VisitorResponse.fromEntity(visitor);
  }

  // Helpers
  private Visitor find(Long id) {
    return visitorRepository
        .findDetailedById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Visitor not found"));
  }

  private VisitorRole findRole(Long roleId) {
    return roleRepository
        .findById(roleId)
        .orElseThrow(() -> new ResourceNotFoundException("Visitor role not found"));
  }

  private SchoolGrade resolveGrade(VisitorRole role, Long gradeId) {
    if (!STUDENT_ROLE_CODE.equals(role.getCode())) {
      if (gradeId != null) {
        throw new BadRequestException("Only students can have a school grade");
      }
      return null;
    }
    if (gradeId == null) {
      throw new BadRequestException("Students must have a school grade");
    }
    return gradeRepository
        .findById(gradeId)
        .orElseThrow(() -> new ResourceNotFoundException("School grade not found"));
  }

  private void assertNameAvailable(String name, SchoolGrade grade, Long selfId) {
    Optional<Visitor> clash =
        grade == null
            ? visitorRepository.findFirstByGradeIsNullAndNameIgnoreCaseAndActiveTrue(name)
            : visitorRepository.findFirstByGradeIdAndNameIgnoreCaseAndActiveTrue(
                grade.getId(), name);

    if (clash.isPresent() && !clash.get().getId().equals(selfId)) {
      throw new ConflictException(
          (grade == null
                  ? "Another active teacher is already named \""
                  : "Another active student in this grade is already named \"")
              + name
              + "\"; rename one of them");
    }
  }
}
