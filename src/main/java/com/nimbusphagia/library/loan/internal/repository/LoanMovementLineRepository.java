package com.nimbusphagia.library.loan.internal.repository;

import com.nimbusphagia.library.loan.internal.entity.LoanMovementLine;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanMovementLineRepository extends JpaRepository<LoanMovementLine, Long> {

  interface LineCount {
    Long getMovementId();

    Long getTotal();

    Long getOpenCount();
  }

  @Query(
      """
      SELECT l.movement.id AS movementId,
             COUNT(l) AS total,
             SUM(CASE WHEN l.resolvedAt IS NULL THEN 1L ELSE 0L END) AS openCount
      FROM LoanMovementLine l
      WHERE l.movement.id IN :ids
      GROUP BY l.movement.id
      """)
  List<LineCount> countByMovementIds(@Param("ids") Collection<Long> ids);

  @EntityGraph(attributePaths = {"copy", "resolvedBy"})
  List<LoanMovementLine> findByMovementIdOrderByIdAsc(Long movementId);

  @Query(
      """
      SELECT COUNT(l) FROM LoanMovementLine l
      WHERE l.movement.loanedTo.id = :visitorId
        AND l.movement.library.id = :libraryId
        AND l.resolvedAt IS NULL
      """)
  long countOpenByVisitorIdAndLibraryId(
      @Param("visitorId") Long visitorId, @Param("libraryId") Long libraryId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT l FROM LoanMovementLine l
      WHERE l.movement.id = :loanId AND l.id IN :ids
      ORDER BY l.id
      """)
  List<LoanMovementLine> findByMovementIdAndIdInForUpdate(
      @Param("loanId") Long loanId, @Param("ids") Set<Long> ids);
}
