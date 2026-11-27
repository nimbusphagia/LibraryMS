package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.dto.copy.CopyFilter;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CopySpecifications {
  private CopySpecifications() {}

  public static Specification<Copy> from(Long libraryId, CopyFilter f) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("library").get("id"), libraryId));
      if (f.bookId() != null) predicates.add(cb.equal(root.get("book").get("id"), f.bookId()));
      if (f.shelfId() != null) predicates.add(cb.equal(root.get("shelf").get("id"), f.shelfId()));
      if (f.status() != null) predicates.add(cb.equal(root.get("status"), f.status()));

      if (Boolean.TRUE.equals(f.unlabeled())) predicates.add(cb.isNull(root.get("placedText")));
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
