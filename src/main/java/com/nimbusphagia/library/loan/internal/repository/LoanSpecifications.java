package com.nimbusphagia.library.loan.internal.repository;

import static com.nimbusphagia.library.loan.internal.util.LoanTime.LIMA;

import com.nimbusphagia.library.loan.internal.dto.loan.LoanFilter;
import com.nimbusphagia.library.loan.internal.entity.LoanMovement;
import com.nimbusphagia.library.loan.internal.entity.LoanMovementLine;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class LoanSpecifications {
  private LoanSpecifications() {}

  public static Specification<LoanMovement> from(Long libraryId, LoanFilter f, Instant now) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("library").get("id"), libraryId));

      if (f.visitorId() != null) {
        predicates.add(cb.equal(root.get("loanedTo").get("id"), f.visitorId()));
      }
      if (f.visitorName() != null) {
        String pattern = "%" + escapeLike(f.visitorName().toLowerCase()) + "%";
        predicates.add(cb.like(cb.lower(root.get("loanedTo").get("name")), pattern, '\\'));
      }
      if (f.createdFrom() != null) {
        predicates.add(
            cb.greaterThanOrEqualTo(
                root.<Instant>get("createdAt"), f.createdFrom().atStartOfDay(LIMA).toInstant()));
      }
      if (f.createdTo() != null) {
        predicates.add(
            cb.lessThan(
                root.<Instant>get("createdAt"),
                f.createdTo().plusDays(1).atStartOfDay(LIMA).toInstant()));
      }
      if (f.status() != null) {
        Subquery<Long> openLines = query.subquery(Long.class);
        Root<LoanMovementLine> line = openLines.from(LoanMovementLine.class);
        openLines
            .select(line.<Long>get("id"))
            .where(cb.equal(line.get("movement"), root), cb.isNull(line.get("resolvedAt")));
        Predicate hasOpenLines = cb.exists(openLines);

        switch (f.status()) {
          case CLOSED -> predicates.add(cb.not(hasOpenLines));
          case OVERDUE -> {
            predicates.add(hasOpenLines);
            predicates.add(cb.lessThan(root.<Instant>get("expiresBy"), now));
          }
          case OPEN -> {
            predicates.add(hasOpenLines);
            predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("expiresBy"), now));
          }
        }
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static String escapeLike(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
