package com.nimbusphagia.library_ms.catalog.internal.repository;

import com.nimbusphagia.library_ms.catalog.internal.dto.book.BookFilter;
import com.nimbusphagia.library_ms.catalog.internal.entity.Book;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecifications {
  private BookSpecifications() {}

  public static Specification<Book> from(BookFilter f) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (f.authorId() != null) {
        predicates.add(cb.equal(root.get("author").get("id"), f.authorId()));
      }
      if (f.illustratorId() != null) {
        predicates.add(cb.equal(root.get("illustrator").get("id"), f.illustratorId()));
      }
      if (f.collaboratorId() != null) {
        predicates.add(hasCollaborator(root, query, cb, f.collaboratorId()));
      }
      if (f.personId() != null) {
        predicates.add(
            cb.or(
                cb.equal(root.get("author").get("id"), f.personId()),
                cb.equal(root.get("illustrator").get("id"), f.personId()),
                hasCollaborator(root, query, cb, f.personId())));
      }
      if (f.publisherId() != null) {
        predicates.add(cb.equal(root.get("publisher").get("id"), f.publisherId()));
      }
      if (f.collectionId() != null) {
        predicates.add(cb.equal(root.get("collection").get("id"), f.collectionId()));
      }
      if (f.deweyId() != null) {
        predicates.add(cb.equal(root.get("dewey").get("id"), f.deweyId()));
      }
      for (Long tagId : f.tagIds()) {
        predicates.add(existsIn(root, query, cb, "tags", t -> cb.equal(t.get("id"), tagId)));
      }
      if (f.country() != null) {
        predicates.add(cb.equal(root.get("country"), f.country()));
      }
      if (f.active() != null) {
        predicates.add(cb.equal(root.get("active"), f.active()));
      }
      if (f.search() != null) {
        predicates.add(searchPredicate(root, query, cb, f.search()));
      }

      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static Predicate searchPredicate(
      Root<Book> root, CriteriaQuery<?> query, CriteriaBuilder cb, String search) {
    String pattern = "%" + escapeLike(search.toLowerCase()) + "%";

    Join<Object, Object> author = root.join("author", JoinType.LEFT);
    Join<Object, Object> illustrator = root.join("illustrator", JoinType.LEFT);
    Join<Object, Object> publisher = root.join("publisher", JoinType.LEFT);
    Join<Object, Object> collection = root.join("collection", JoinType.LEFT);
    Join<Object, Object> dewey = root.join("dewey", JoinType.LEFT);

    return cb.or(
        like(cb, root.get("title"), pattern),
        like(cb, root.get("isbn"), pattern),
        like(cb, root.get("country"), pattern),
        like(cb, fullName(cb, author), pattern),
        like(cb, fullName(cb, illustrator), pattern),
        like(cb, publisher.get("name"), pattern),
        like(cb, collection.get("name"), pattern),
        like(cb, dewey.get("code"), pattern),
        like(cb, dewey.get("name"), pattern),
        existsIn(root, query, cb, "collaborators", c -> like(cb, fullName(cb, c), pattern)),
        existsIn(root, query, cb, "tags", t -> like(cb, t.get("name"), pattern)));
  }

  private static Predicate hasCollaborator(
      Root<Book> root, CriteriaQuery<?> query, CriteriaBuilder cb, Long authorId) {
    return existsIn(root, query, cb, "collaborators", c -> cb.equal(c.get("id"), authorId));
  }

  private static Predicate existsIn(
      Root<Book> root,
      CriteriaQuery<?> query,
      CriteriaBuilder cb,
      String attribute,
      Function<Join<Book, Object>, Predicate> condition) {
    Subquery<Long> sub = query.subquery(Long.class);
    Root<Book> book = sub.from(Book.class);
    Join<Book, Object> join = book.join(attribute);
    sub.select(book.<Long>get("id"))
        .where(cb.equal(book.get("id"), root.get("id")), condition.apply(join));
    return cb.exists(sub);
  }

  private static Expression<String> fullName(CriteriaBuilder cb, From<?, ?> author) {
    return cb.concat(
        cb.concat(author.<String>get("firstName"), " "),
        cb.coalesce(author.<String>get("lastName"), ""));
  }

  private static Predicate like(CriteriaBuilder cb, Expression<String> expr, String pattern) {
    return cb.like(cb.lower(expr), pattern, '\\');
  }

  private static String escapeLike(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
