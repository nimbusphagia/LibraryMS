package com.nimbusphagia.library.catalog.internal.repository;

import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.contains;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.fullName;
import static com.nimbusphagia.library.catalog.internal.repository.CatalogSpecifications.pattern;

import com.nimbusphagia.library.catalog.internal.dto.book.BookFilter;
import com.nimbusphagia.library.catalog.internal.entity.Book;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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
      if (f.seriesId() != null) {
        predicates.add(cb.equal(root.get("series").get("id"), f.seriesId()));
      }
      if (f.categoryId() != null) {
        predicates.add(cb.equal(root.get("category").get("id"), f.categoryId()));
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
    String pattern = pattern(search);

    Join<Object, Object> author = root.join("author", JoinType.LEFT);
    Join<Object, Object> illustrator = root.join("illustrator", JoinType.LEFT);
    Join<Object, Object> publisher = root.join("publisher", JoinType.LEFT);
    Join<Object, Object> collection = root.join("collection", JoinType.LEFT);
    Join<Object, Object> series = root.join("series", JoinType.LEFT);
    Join<Object, Object> category = root.join("category", JoinType.LEFT);
    Join<Object, Object> dewey = root.join("dewey", JoinType.LEFT);

    return cb.or(
        contains(cb, root.get("title"), pattern),
        contains(cb, root.get("isbn"), pattern),
        contains(cb, root.get("country"), pattern),
        contains(cb, fullName(cb, author), pattern),
        contains(cb, fullName(cb, illustrator), pattern),
        contains(cb, publisher.get("name"), pattern),
        contains(cb, collection.get("name"), pattern),
        contains(cb, collection.get("code"), pattern),
        contains(cb, series.get("name"), pattern),
        contains(cb, category.get("name"), pattern),
        contains(cb, category.get("code"), pattern),
        contains(cb, dewey.get("code"), pattern),
        contains(cb, dewey.get("name"), pattern),
        existsIn(root, query, cb, "collaborators", c -> contains(cb, fullName(cb, c), pattern)),
        existsIn(root, query, cb, "tags", t -> contains(cb, t.get("name"), pattern)));
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
}
