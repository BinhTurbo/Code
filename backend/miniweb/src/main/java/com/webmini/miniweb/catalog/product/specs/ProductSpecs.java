package com.webmini.miniweb.catalog.product.specs;

import com.webmini.miniweb.catalog.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;

public class ProductSpecs {
    public static Specification<Product> qLike(String q) {
        return (root, cq, cb) -> (q == null || q.isBlank()) ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%");
    }
    public static Specification<Product> skuEquals(String sku) {
        return (root, cq, cb) -> (sku == null || sku.isBlank()) ? cb.conjunction()
                : cb.equal(cb.lower(root.get("sku")), sku.toLowerCase());
    }
    public static Specification<Product> categoryIdEquals(Long categoryId) {
        return (root, cq, cb) -> (categoryId == null) ? cb.conjunction()
                : cb.equal(root.get("category").get("id"), categoryId);
    }
    public static Specification<Product> statusEquals(String status) {
        return (root, cq, cb) -> (status == null || status.isBlank()) ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }
    public static Specification<Product> stockLt(Integer minStockLt) {
        return (root, cq, cb) -> (minStockLt == null) ? cb.conjunction()
                : cb.lessThan(root.get("stock"), minStockLt);
    }
    public static Specification<Product> priceGte(BigDecimal priceGte) {
        return (root, cq, cb) -> (priceGte == null) ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("price"), priceGte);
    }
}