package com.webmini.miniweb.catalog.product.repo;

import com.webmini.miniweb.catalog.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsByCategory_Id(Long categoryId);

    List<Product> findAllByCategory_Id(Long categoryId);

    @Query("SELECT p FROM Product p JOIN FETCH p.category ORDER BY p.id")
    Page<Product> findAllWithCategory(Pageable pageable);
}