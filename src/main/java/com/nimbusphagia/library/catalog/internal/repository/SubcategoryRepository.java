package com.nimbusphagia.library.catalog.internal.repository;

import com.nimbusphagia.library.catalog.internal.entity.Subcategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubcategoryRepository
    extends JpaRepository<Subcategory, Long>, JpaSpecificationExecutor<Subcategory> {
  Optional<Subcategory> findByIdAndCategoryId(Long id, Long categoryId);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM book WHERE subcategory_id = :id
          )
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("id") Long id);
}
