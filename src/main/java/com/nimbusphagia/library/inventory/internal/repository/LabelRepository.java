package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.Label;
import com.nimbusphagia.library.inventory.internal.entity.PrintStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabelRepository extends JpaRepository<Label, Long> {

  boolean existsByCopyId(Long copyId);

  @EntityGraph(attributePaths = {"library", "book", "printedBy"})
  Optional<Label> findByCopyId(Long copyId);

  @EntityGraph(attributePaths = {"library", "book", "printedBy"})
  List<Label> findByIdIn(Set<Long> ids);

  @EntityGraph(attributePaths = {"library", "book", "printedBy"})
  @Query(
      """
      SELECT l FROM Label l
      WHERE l.library.id = :libraryId
        AND l.printStatus = :status
        AND l.retiredAt IS NULL
      ORDER BY l.id
      """)
  List<Label> findActiveByLibraryAndPrintStatus(
      @Param("libraryId") Long libraryId, @Param("status") PrintStatus status);
}
