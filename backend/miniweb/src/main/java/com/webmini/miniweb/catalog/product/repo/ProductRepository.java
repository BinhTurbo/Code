package com.webmini.miniweb.catalog.product.repo;

import com.webmini.miniweb.catalog.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(Long id);
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsByCategoryId(Long categoryId);
    List<Product> findAllByCategoryId(Long categoryId);
    Page<Product> findAllWithCategory(Pageable pageable);
    boolean existsById(Long id);
    void deleteById(Long id);
    Page<Product> search(String q, String sku, Long categoryId, String status, Integer minStockLt, Pageable pageable);
    List<Product> findAll();
    void saveAll(List<Product> products);
    
    List<Product> findByCategoryIdAndStatus(Long categoryId, Product.ProductStatus status);
}