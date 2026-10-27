package com.nimbusphagia.library_ms.catalog.internal.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.nimbusphagia.library_ms.catalog.internal.entity.Book;

/**
 * BookRepository
 */
public interface BookRepository extends JpaRepository<Book, Long> {

	
}
