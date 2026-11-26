package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.CopyConditionEvent;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CopyConditionEventRepository extends JpaRepository<CopyConditionEvent, Long> {

  @EntityGraph(attributePaths = {"actor"})
  List<CopyConditionEvent> findByCopyIdOrderByIdDesc(Long copyId);
}
