package com.nimbusphagia.library_ms.catalog.internal.repository;

import com.nimbusphagia.library_ms.catalog.internal.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

/** AuthorRepository */
public interface AuthorRepository extends JpaRepository<Author, Long> {}
