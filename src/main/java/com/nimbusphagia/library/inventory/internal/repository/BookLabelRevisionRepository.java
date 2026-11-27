package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.BookLabelRevision;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookLabelRevisionRepository extends JpaRepository<BookLabelRevision, Long> {

  @EntityGraph(attributePaths = {"changedBy"})
  List<BookLabelRevision> findByBookLabelIdOrderByRevisionNumberDesc(Long bookLabelId);

  boolean existsBySchemeId(Long schemeId);
}
