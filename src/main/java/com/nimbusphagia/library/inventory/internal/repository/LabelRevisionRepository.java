package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.LabelRevision;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRevisionRepository extends JpaRepository<LabelRevision, Long> {

  @EntityGraph(attributePaths = "changedBy")
  List<LabelRevision> findByLabelIdOrderByRevisionNumberDesc(Long labelId);
}
