package com.nimbusphagia.library.catalog.internal.repository;

import com.nimbusphagia.library.catalog.internal.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository
    extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM book WHERE category_id = :id
          ) OR EXISTS (
            SELECT 1 FROM subcategory WHERE category_id = :id
          )
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("id") Long id);
}
