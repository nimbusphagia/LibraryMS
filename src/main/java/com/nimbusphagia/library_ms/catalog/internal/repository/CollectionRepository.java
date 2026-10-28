package com.nimbusphagia.library_ms.catalog.internal.repository;

import com.nimbusphagia.library_ms.catalog.internal.entity.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
  Optional<Collection> findByIdAndPublisherId(Long id, Long publisherId);

  @Query(
      """
      SELECT COUNT(c) > 0 FROM Collection c
      WHERE c.publisher.id = :publisherId AND LOWER(c.name) = LOWER(:name)
      """)
  boolean existsByPublisherAndName(
      @Param("publisherId") Long publisherId, @Param("name") String name);

  @Query(
      """
      SELECT COUNT(c) > 0 FROM Collection c
      WHERE c.publisher.id = :publisherId AND LOWER(c.name) = LOWER(:name) AND c.id <> :id
      """)
  boolean existsByPublisherAndNameAndIdNot(
      @Param("publisherId") Long publisherId, @Param("name") String name, @Param("id") Long id);

  Page<Collection> findByPublisherId(Long publisherId, Pageable pageable);

  Page<Collection> findByPublisherIdAndNameContainingIgnoreCase(
      Long publisherId, String name, Pageable pageable);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM book WHERE collection_id = :id
          )
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("id") Long id);
}
