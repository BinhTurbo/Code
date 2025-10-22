package com.webmini.miniweb.catalog.category.repo;

import com.webmini.miniweb.catalog.category.entity.Category;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Category> findByNameIgnoreCase(String name);
}