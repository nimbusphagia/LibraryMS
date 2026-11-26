package com.nimbusphagia.library.loan.internal.service;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.loan.internal.dto.incident.IncidentRequest;
import com.nimbusphagia.library.loan.internal.dto.incident.IncidentResponse;
import com.nimbusphagia.library.loan.internal.dto.incident.IncidentTypeRequest;
import com.nimbusphagia.library.loan.internal.dto.incident.IncidentTypeResponse;
import com.nimbusphagia.library.loan.internal.entity.LoanMovementLine;
import com.nimbusphagia.library.loan.internal.entity.Visitor;
import com.nimbusphagia.library.loan.internal.entity.VisitorIncident;
import com.nimbusphagia.library.loan.internal.entity.VisitorIncidentType;
import com.nimbusphagia.library.loan.internal.repository.LoanMovementLineRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorIncidentRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorIncidentTypeRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncidentService {
  private final VisitorIncidentTypeRepository typeRepository;
  private final VisitorIncidentRepository incidentRepository;
  private final VisitorRepository visitorRepository;
  private final LoanMovementLineRepository lineRepository;
  private final EntityManager entityManager;

  @Transactional(readOnly = true)
  public List<IncidentTypeResponse> findTypes(Long institutionId) {
    return typeRepository.findByInstitutionIdOrderByNameAsc(institutionId).stream()
        .map(IncidentTypeResponse::fromEntity)
        .toList();
  }

  @Transactional
  public IncidentTypeResponse createType(Long institutionId, IncidentTypeRequest request) {
    if (typeRepository.existsByInstitutionIdAndNameIgnoreCase(institutionId, request.name())) {
      throw new ConflictException("Incident type name already exists");
    }
    VisitorIncidentType type = VisitorIncidentType.builder().institutionId(institutionId).build();
    return IncidentTypeResponse.fromEntity(typeRepository.save(request.replace(type)));
  }

  @Transactional
  public IncidentTypeResponse editType(Long institutionId, Long id, IncidentTypeRequest request) {
    VisitorIncidentType type = findType(institutionId, id);
    if (typeRepository.existsByInstitutionIdAndNameIgnoreCaseAndIdNot(
        institutionId, request.name(), id)) {
      throw new ConflictException("Incident type name already exists");
    }
    return IncidentTypeResponse.fromEntity(request.replace(type));
  }

  @Transactional(readOnly = true)
  public List<IncidentResponse> findByVisitor(Long institutionId, Long visitorId) {
    Visitor visitor = findVisitor(institutionId, visitorId);
    return incidentRepository.findByVisitorIdOrderByCreatedAtDesc(visitor.getId()).stream()
        .map(IncidentResponse::fromEntity)
        .toList();
  }

  @Transactional
  public IncidentResponse record(
      Long institutionId, Long visitorId, IncidentRequest request, Long actorId) {
    Visitor visitor = findVisitor(institutionId, visitorId);
    VisitorIncidentType type = findType(institutionId, request.typeId());
    if (!type.isActive()) {
      throw new ConflictException("This incident type is inactive");
    }
    LoanMovementLine line =
        request.loanLineId() == null
            ? null
            : lineRepository
                .findById(request.loanLineId())
                .filter(l -> l.getMovement().getLoanedTo().getId().equals(visitorId))
                .orElseThrow(
                    () -> new ResourceNotFoundException("Loan line not found for this visitor"));

    VisitorIncident incident =
        incidentRepository.save(
            VisitorIncident.builder()
                .institutionId(institutionId)
                .visitor(visitor)
                .type(type)
                .loanLine(line)
                .recordedBy(entityManager.find(Account.class, actorId))
                .notes(request.notes())
                .build());
    return IncidentResponse.fromEntity(incident);
  }

  @Transactional
  public void delete(Long institutionId, Long visitorId, Long id) {
    VisitorIncident incident =
        incidentRepository
            .findByIdAndVisitorIdAndInstitutionId(id, visitorId, institutionId)
            .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
    incidentRepository.delete(incident);
  }

  // Helpers
  private VisitorIncidentType findType(Long institutionId, Long id) {
    return typeRepository
        .findByIdAndInstitutionId(id, institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Incident type not found"));
  }

  private Visitor findVisitor(Long institutionId, Long id) {
    return visitorRepository
        .findByIdAndInstitutionId(id, institutionId)
        .orElseThrow(() -> new ResourceNotFoundException("Visitor not found"));
  }
}
