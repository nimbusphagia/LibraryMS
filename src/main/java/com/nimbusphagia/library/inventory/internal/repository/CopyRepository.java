package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CopyRepository extends JpaRepository<Copy, Long> {

  @EntityGraph(attributePaths = {"library", "book"})
  @Query("SELECT c FROM Copy c WHERE c.id = :id")
  Optional<Copy> findDetailedById(@Param("id") Long id);

  @Query(
      """
      SELECT COALESCE(MAX(c.iteration), 0) FROM Copy c
      WHERE c.library.id = :libraryId
        AND c.book.id = :bookId
        AND c.status IN :statuses
      """)
  int findMaxHeldIteration(
      @Param("libraryId") Long libraryId,
      @Param("bookId") Long bookId,
      @Param("statuses") Set<CopyStatus> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM Copy c WHERE c.id IN :ids")
  List<Copy> findAllByIdInForUpdate(@Param("ids") Set<Long> ids);

  boolean existsByShelfId(Long shelfId);
}
