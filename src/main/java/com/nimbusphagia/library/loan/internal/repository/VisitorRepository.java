package com.nimbusphagia.library.loan.internal.repository;

import com.nimbusphagia.library.loan.internal.entity.Visitor;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitorRepository
    extends JpaRepository<Visitor, Long>, JpaSpecificationExecutor<Visitor> {

  @Override
  @EntityGraph(attributePaths = {"role", "grade"})
  Page<Visitor> findAll(Specification<Visitor> spec, Pageable pageable);

  @EntityGraph(attributePaths = {"role", "grade"})
  @Query("SELECT v FROM Visitor v WHERE v.id = :id")
  Optional<Visitor> findDetailedById(@Param("id") Long id);

  Optional<Visitor> findFirstByGradeIdAndNameIgnoreCaseAndActiveTrue(Long gradeId, String name);

  Optional<Visitor> findFirstByGradeIsNullAndNameIgnoreCaseAndActiveTrue(String name);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT v FROM Visitor v WHERE v.id = :id")
  Optional<Visitor> findByIdForUpdate(@Param("id") Long id);
}
