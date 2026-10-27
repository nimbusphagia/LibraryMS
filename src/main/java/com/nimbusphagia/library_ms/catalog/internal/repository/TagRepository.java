package com.nimbusphagia.library_ms.catalog.internal.repository;

import com.nimbusphagia.library_ms.catalog.internal.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

/** TagRepository */
public interface TagRepository extends JpaRepository<Tag, Long> {}
