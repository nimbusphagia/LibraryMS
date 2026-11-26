package com.nimbusphagia.library.loan.internal.repository;

import com.nimbusphagia.library.loan.internal.entity.LoanMovementLine;
import jakarta.persistence.LockModeType;
import java.time.Instant;
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

  @Query(
      """
      SELECT COUNT(l) FROM LoanMovementLine l
      WHERE l.movement.loanedTo.id = :visitorId
        AND l.movement.library.id = :libraryId
        AND l.movement.expiresBy < :cutoff
        AND l.resolvedAt IS NULL
      """)
  long countOverdueByVisitorIdAndLibraryId(
      @Param("visitorId") Long visitorId,
      @Param("libraryId") Long libraryId,
      @Param("cutoff") Instant cutoff);

  interface History {
    Long getVisitorId();

    long getResolved();

    long getLate();

    long getDamaged();

    long getLost();
  }

  // Each line falls in one category; a recovered loss counts as late
  @Query(
      nativeQuery = true,
      value =
          """
          SELECT m.loaned_to_id AS visitorId,
                 COUNT(*) AS resolved,
                 COUNT(*) FILTER (WHERE (l.outcome = 'RETURNED' AND l.days_late > r.grace_days)
                                     OR (l.outcome = 'LOST' AND rec.id IS NOT NULL)) AS late,
                 COUNT(*) FILTER (WHERE l.outcome IN ('RETURNED_DAMAGED', 'DISCARDED')) AS damaged,
                 COUNT(*) FILTER (WHERE l.outcome = 'LOST' AND rec.id IS NULL) AS lost
          FROM loan_movement_line l
          JOIN loan_movement m ON m.id = l.loan_movement_id
          JOIN visitor v ON v.id = m.loaned_to_id
          JOIN visitor_role r ON r.id = v.role_id
          LEFT JOIN stock_movement_line ex ON ex.loan_line_id = l.id
          LEFT JOIN stock_movement_line rec ON rec.recovered_line_id = ex.id
          WHERE m.loaned_to_id IN (:visitorIds) AND l.resolved_at >= :since
          GROUP BY m.loaned_to_id
          """)
  List<History> findHistory(
      @Param("visitorIds") Collection<Long> visitorIds, @Param("since") Instant since);

  boolean existsByMovementLoanedToIdAndResolvedAtIsNull(Long visitorId);

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
