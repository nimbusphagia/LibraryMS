package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.StockMovementLine;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementLineRepository extends JpaRepository<StockMovementLine, Long> {

  interface LineCount {
    Long getMovementId();

    Long getTotal();
  }

  @Query(
      """
      SELECT l.movement.id AS movementId, COUNT(l) AS total
      FROM StockMovementLine l
      WHERE l.movement.id IN :ids
      GROUP BY l.movement.id
      """)
  List<LineCount> countByMovementIds(@Param("ids") Collection<Long> ids);

  @EntityGraph(attributePaths = {"copy", "copy.shelf"})
  List<StockMovementLine> findByMovementIdOrderByCopyBookIdAscCopyIterationAsc(Long movementId);
}
