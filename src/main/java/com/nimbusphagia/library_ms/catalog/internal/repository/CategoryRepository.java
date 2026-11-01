package com.nimbusphagia.library_ms.catalog.internal.repository;

import com.nimbusphagia.library_ms.catalog.internal.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {
  @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
  boolean existsByNameIgnoreCase(@Param("name") String name);

  @Query(
      """
      SELECT COUNT(c) > 0 FROM Category c
      WHERE LOWER(c.name) = LOWER(:name) AND c.id <> :id
      """)
  boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);

  @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.code) = LOWER(:code)")
  boolean existsByCodeIgnoreCase(@Param("code") String code);

  @Query(
      """
      SELECT COUNT(c) > 0 FROM Category c
      WHERE LOWER(c.code) = LOWER(:code) AND c.id <> :id
      """)
  boolean existsByCodeIgnoreCaseAndIdNot(@Param("code") String code, @Param("id") Long id);

  @Query(
      """
      SELECT c FROM Category c
      WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))
      """)
  Page<Category> search(@Param("search") String search, Pageable pageable);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM book WHERE category_id = :id
          )
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("id") Long id);
}
