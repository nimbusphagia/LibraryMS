package com.nimbusphagia.library.catalog.internal.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class CatalogSpecifications {
  private CatalogSpecifications() {}

  // Case-insensitive, like the unique indexes
  public static <T> Specification<T> textEquals(String attribute, String value) {
    return (root, query, cb) ->
        value == null
            ? cb.isNull(root.get(attribute))
            : cb.equal(cb.lower(root.get(attribute)), value.toLowerCase());
  }

  // A null id, as on creation, excludes nothing
  public static <T> Specification<T> idNot(Long id) {
    return (root, query, cb) -> id == null ? cb.conjunction() : cb.notEqual(root.get("id"), id);
  }

  public static <T> Specification<T> attributeIs(String attribute, Long id) {
    return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
  }

  // A blank search matches everything
  public static <T> Specification<T> anyContains(String search, String... attributes) {
    return (root, query, cb) -> {
      if (search == null || search.isBlank()) {
        return cb.conjunction();
      }
      String pattern = pattern(search);
      Predicate[] predicates = new Predicate[attributes.length];
      for (int i = 0; i < attributes.length; i++) {
        predicates[i] = contains(cb, root.get(attributes[i]), pattern);
      }
      return cb.or(predicates);
    };
  }

  public static String pattern(String search) {
    return "%" + escapeLike(search.strip()) + "%";
  }

  // Case- and accent-insensitive LIKE against a pattern built by pattern()
  public static Predicate contains(CriteriaBuilder cb, Expression<String> expr, String pattern) {
    return cb.like(cb.lower(unaccent(cb, expr)), cb.lower(unaccent(cb, cb.literal(pattern))), '\\');
  }

  public static Expression<String> fullName(CriteriaBuilder cb, From<?, ?> author) {
    return cb.concat(
        cb.concat(author.<String>get("firstName"), " "),
        cb.coalesce(author.<String>get("lastName"), ""));
  }

  private static Expression<String> unaccent(CriteriaBuilder cb, Expression<String> expr) {
    return cb.function("unaccent", String.class, expr);
  }

  public static String escapeLike(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
