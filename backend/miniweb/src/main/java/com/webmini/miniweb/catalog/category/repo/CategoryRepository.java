package com.webmini.miniweb.catalog.category.repo;

import com.webmini.miniweb.catalog.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findById(Long id);
    boolean existsByNameIgnoreCase(String name);
    Optional<Category> findByNameIgnoreCase(String name);
    boolean existsById(Long id);
    void deleteById(Long id);
    Page<Category> search(String q, String status, Pageable pageable);
}