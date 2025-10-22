package com.webmini.miniweb.catalog.category.specs;

import com.webmini.miniweb.catalog.category.entity.Category;
import org.springframework.data.jpa.domain.Specification;

public class CategorySpecs {
    public static Specification<Category> nameContains(String q) {
        return (root, cq, cb) -> (q == null || q.isBlank()) ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%");
    }
    public static Specification<Category> statusEquals(String status) {
        return (root, cq, cb) -> (status == null || status.isBlank()) ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }
}