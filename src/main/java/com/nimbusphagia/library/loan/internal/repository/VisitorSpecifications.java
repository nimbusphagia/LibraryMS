package com.nimbusphagia.library.loan.internal.repository;

import com.nimbusphagia.library.loan.internal.dto.visitor.VisitorFilter;
import com.nimbusphagia.library.loan.internal.entity.Visitor;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class VisitorSpecifications {
  private VisitorSpecifications() {}

  public static Specification<Visitor> from(VisitorFilter f) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (f.search() != null) {
        String pattern = "%" + escapeLike(f.search().toLowerCase()) + "%";
        predicates.add(cb.like(cb.lower(root.get("name")), pattern, '\\'));
      }
      if (f.roleId() != null) {
        predicates.add(cb.equal(root.get("role").get("id"), f.roleId()));
      }
      if (f.gradeId() != null) {
        predicates.add(cb.equal(root.get("grade").get("id"), f.gradeId()));
      }
      if (f.active() != null) {
        predicates.add(cb.equal(root.get("active"), f.active()));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static String escapeLike(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
