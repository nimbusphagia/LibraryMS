package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.Label;
import com.nimbusphagia.library.inventory.internal.entity.PrintStatus;
import java.util.Collection;
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

  List<Label> findByLibraryIdAndBookIdAndRetiredAtIsNull(Long libraryId, Long bookId);

  List<Label> findByCopyIdInAndRetiredAtIsNull(Collection<Long> copyIds);

  // An active label of another book shows the text, unless it is OUTDATED and waits to change
  @Query(
      """
      SELECT COUNT(l) > 0 FROM Label l
      WHERE l.library.id = :libraryId
        AND LOWER(l.text) = LOWER(:text)
        AND l.retiredAt IS NULL
        AND l.printStatus <> com.nimbusphagia.library.inventory.internal.entity.PrintStatus.OUTDATED
        AND l.book.id <> :bookId
      """)
  boolean isTextTakenByOtherBook(
      @Param("libraryId") Long libraryId,
      @Param("text") String text,
      @Param("bookId") Long bookId);
}
