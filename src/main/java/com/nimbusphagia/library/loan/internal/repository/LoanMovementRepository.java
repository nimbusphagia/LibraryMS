package com.nimbusphagia.library.loan.internal.repository;

import com.nimbusphagia.library.loan.internal.entity.LoanMovement;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanMovementRepository
    extends JpaRepository<LoanMovement, Long>, JpaSpecificationExecutor<LoanMovement> {

  @Override
  @EntityGraph(attributePaths = {"loanedTo", "loanedBy"})
  Page<LoanMovement> findAll(Specification<LoanMovement> spec, Pageable pageable);

  @EntityGraph(
      attributePaths = {"library", "loanedTo", "loanedTo.role", "loanedTo.grade", "loanedBy"})
  @Query("SELECT m FROM LoanMovement m WHERE m.id = :id")
  Optional<LoanMovement> findDetailedById(@Param("id") Long id);
}
