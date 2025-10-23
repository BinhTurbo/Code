package com.webmini.miniweb.catalog.product.repo;

import com.webmini.miniweb.catalog.category.entity.Category;
import com.webmini.miniweb.catalog.product.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepositoryImpl implements ProductRepository {
    
    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public Product save(Product product) {
        if (product.getId() == null) {
            // Insert
            String sql = """
                INSERT INTO products (sku, name, category_id, price, stock, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            em.createNativeQuery(sql)
                .setParameter(1, product.getSku())
                .setParameter(2, product.getName())
                .setParameter(3, product.getCategory().getId())
                .setParameter(4, product.getPrice())
                .setParameter(5, product.getStock())
                .setParameter(6, product.getStatus().name())
                .setParameter(7, product.getCreatedAt() != null ? product.getCreatedAt() : LocalDateTime.now())
                .setParameter(8, product.getUpdatedAt())
                .executeUpdate();
            
            // Get generated ID
            Long id = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
            product.setId(id);
            return product;
        } else {
            // Update
            String sql = """
                UPDATE products
                SET sku = ?, name = ?, category_id = ?, price = ?, stock = ?, status = ?, updated_at = ?
                WHERE id = ?
            """;
            em.createNativeQuery(sql)
                .setParameter(1, product.getSku())
                .setParameter(2, product.getName())
                .setParameter(3, product.getCategory().getId())
                .setParameter(4, product.getPrice())
                .setParameter(5, product.getStock())
                .setParameter(6, product.getStatus().name())
                .setParameter(7, product.getUpdatedAt())
                .setParameter(8, product.getId())
                .executeUpdate();
            return product;
        }
    }

    @Override
    public Optional<Product> findById(Long id) {
        String sql = """
            SELECT p.id, p.sku, p.name, p.price, p.stock, p.status, p.created_at, p.updated_at,
                   c.id as cat_id, c.name as cat_name, c.status as cat_status, c.created_at as cat_created, c.updated_at as cat_updated
            FROM products p
            INNER JOIN categories c ON p.category_id = c.id
            WHERE p.id = ?
        """;
        
        try {
            Object[] result = (Object[]) em.createNativeQuery(sql)
                .setParameter(1, id)
                .getSingleResult();
            
            return Optional.of(mapToProduct(result));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsBySkuIgnoreCase(String sku) {
        String sql = "SELECT COUNT(*) FROM products WHERE LOWER(sku) = LOWER(?)";
        Long count = ((Number) em.createNativeQuery(sql)
            .setParameter(1, sku)
            .getSingleResult()).longValue();
        return count > 0;
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        String sql = "SELECT COUNT(*) FROM products WHERE category_id = ?";
        Long count = ((Number) em.createNativeQuery(sql)
            .setParameter(1, categoryId)
            .getSingleResult()).longValue();
        return count > 0;
    }

    @Override
    public List<Product> findAllByCategoryId(Long categoryId) {
        String sql = """
            SELECT p.id, p.sku, p.name, p.price, p.stock, p.status, p.created_at, p.updated_at,
                   c.id as cat_id, c.name as cat_name, c.status as cat_status, c.created_at as cat_created, c.updated_at as cat_updated
            FROM products p
            INNER JOIN categories c ON p.category_id = c.id
            WHERE p.category_id = ?
            ORDER BY p.id
        """;
        
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
            .setParameter(1, categoryId)
            .getResultList();
        
        return rows.stream().map(this::mapToProduct).toList();
    }

    @Override
    public Page<Product> findAllWithCategory(Pageable pageable) {
        // Count total
        String countSql = "SELECT COUNT(*) FROM products";
        Long total = ((Number) em.createNativeQuery(countSql).getSingleResult()).longValue();
        
        // Get data with pagination
        String dataSql = """
            SELECT p.id, p.sku, p.name, p.price, p.stock, p.status, p.created_at, p.updated_at,
                   c.id as cat_id, c.name as cat_name, c.status as cat_status, c.created_at as cat_created, c.updated_at as cat_updated
            FROM products p
            INNER JOIN categories c ON p.category_id = c.id
            ORDER BY p.id
            LIMIT ? OFFSET ?
        """;
        
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(dataSql)
            .setParameter(1, pageable.getPageSize())
            .setParameter(2, pageable.getOffset())
            .getResultList();
        
        List<Product> products = rows.stream().map(this::mapToProduct).toList();
        
        return new PageImpl<>(products, pageable, total);
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM products WHERE id = ?";
        Long count = ((Number) em.createNativeQuery(sql)
            .setParameter(1, id)
            .getSingleResult()).longValue();
        return count > 0;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        em.createNativeQuery(sql)
            .setParameter(1, id)
            .executeUpdate();
    }

    @Override
    public Page<Product> search(String q, String sku, Long categoryId, String status, Integer minStockLt, Pageable pageable) {
        // Build WHERE clause
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        int paramIndex = 1;
        
        if (q != null && !q.isBlank()) {
            whereClause.append(" AND LOWER(p.name) LIKE ?").append(paramIndex);
            params.add("%" + q.toLowerCase() + "%");
            paramIndex++;
        }
        
        if (sku != null && !sku.isBlank()) {
            whereClause.append(" AND LOWER(p.sku) = LOWER(?").append(paramIndex).append(")");
            params.add(sku);
            paramIndex++;
        }
        
        if (categoryId != null) {
            whereClause.append(" AND p.category_id = ?").append(paramIndex);
            params.add(categoryId);
            paramIndex++;
        }
        
        if (status != null && !status.isBlank()) {
            whereClause.append(" AND p.status = ?").append(paramIndex);
            params.add(status);
            paramIndex++;
        }
        
        if (minStockLt != null) {
            whereClause.append(" AND p.stock < ?").append(paramIndex);
            params.add(minStockLt);
            paramIndex++;
        }
        
        // Count total
        String countSql = "SELECT COUNT(*) FROM products p" + whereClause;
        var countQuery = em.createNativeQuery(countSql);
        for (int i = 0; i < params.size(); i++) {
            countQuery.setParameter(i + 1, params.get(i));
        }
        Long total = ((Number) countQuery.getSingleResult()).longValue();
        
        // Get data with pagination
        String dataSql = """
            SELECT p.id, p.sku, p.name, p.price, p.stock, p.status, p.created_at, p.updated_at,
                   c.id as cat_id, c.name as cat_name, c.status as cat_status, c.created_at as cat_created, c.updated_at as cat_updated
            FROM products p
            INNER JOIN categories c ON p.category_id = c.id
        """ + whereClause + " ORDER BY " + buildOrderBy(pageable) + " LIMIT ?" + paramIndex + " OFFSET ?" + (paramIndex + 1);
        
        var dataQuery = em.createNativeQuery(dataSql);
        for (int i = 0; i < params.size(); i++) {
            dataQuery.setParameter(i + 1, params.get(i));
        }
        dataQuery.setParameter(paramIndex, pageable.getPageSize());
        dataQuery.setParameter(paramIndex + 1, (int) pageable.getOffset());
        
        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<Product> products = rows.stream().map(this::mapToProduct).toList();
        
        return new PageImpl<>(products, pageable, total);
    }

    @Override
    public List<Product> findAll() {
        String sql = """
            SELECT p.id, p.sku, p.name, p.price, p.stock, p.status, p.created_at, p.updated_at,
                   c.id as cat_id, c.name as cat_name, c.status as cat_status, c.created_at as cat_created, c.updated_at as cat_updated
            FROM products p
            INNER JOIN categories c ON p.category_id = c.id
            ORDER BY p.id
        """;
        
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        
        return rows.stream().map(this::mapToProduct).toList();
    }

    @Override
    @Transactional
    public void saveAll(List<Product> products) {
        for (Product product : products) {
            if (product.getId() == null) {
                // Insert
                String sql = """
                    INSERT INTO products (sku, name, category_id, price, stock, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
                em.createNativeQuery(sql)
                    .setParameter(1, product.getSku())
                    .setParameter(2, product.getName())
                    .setParameter(3, product.getCategory().getId())
                    .setParameter(4, product.getPrice())
                    .setParameter(5, product.getStock())
                    .setParameter(6, product.getStatus().name())
                    .setParameter(7, product.getCreatedAt() != null ? product.getCreatedAt() : LocalDateTime.now())
                    .setParameter(8, product.getUpdatedAt())
                    .executeUpdate();
                
                Long id = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
                product.setId(id);
            } else {
                // Update
                String sql = """
                    UPDATE products
                    SET sku = ?, name = ?, category_id = ?, price = ?, stock = ?, status = ?, updated_at = ?
                    WHERE id = ?
                """;
                em.createNativeQuery(sql)
                    .setParameter(1, product.getSku())
                    .setParameter(2, product.getName())
                    .setParameter(3, product.getCategory().getId())
                    .setParameter(4, product.getPrice())
                    .setParameter(5, product.getStock())
                    .setParameter(6, product.getStatus().name())
                    .setParameter(7, product.getUpdatedAt())
                    .setParameter(8, product.getId())
                    .executeUpdate();
            }
        }
    }

    private String buildOrderBy(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            StringBuilder orderBy = new StringBuilder();
            pageable.getSort().forEach(order -> {
                if (!orderBy.isEmpty()) orderBy.append(", ");
                // Map Java property names to database column names
                String columnName = mapPropertyToColumn(order.getProperty());
                orderBy.append("p.").append(columnName).append(" ").append(order.getDirection().name());
            });
            return orderBy.toString();
        }
        return "p.id DESC";
    }
    
    private String mapPropertyToColumn(String property) {
        return switch (property) {
            case "createdAt" -> "created_at";
            case "updatedAt" -> "updated_at";
            case "categoryId" -> "category_id";
            default -> property; // id, sku, name, price, stock, status remain the same
        };
    }

    private Product mapToProduct(Object[] row) {
        Product product = new Product();
        product.setId(((Number) row[0]).longValue());
        product.setSku((String) row[1]);
        product.setName((String) row[2]);
        product.setPrice((BigDecimal) row[3]);
        product.setStock((Integer) row[4]);
        product.setStatus(Product.ProductStatus.valueOf((String) row[5]));
        
        // Convert java.sql.Timestamp to LocalDateTime
        if (row[6] != null) {
            product.setCreatedAt(((java.sql.Timestamp) row[6]).toLocalDateTime());
        }
        if (row[7] != null) {
            product.setUpdatedAt(((java.sql.Timestamp) row[7]).toLocalDateTime());
        }
        
        Category category = new Category();
        category.setId(((Number) row[8]).longValue());
        category.setName((String) row[9]);
        category.setStatus(Category.CategoryStatus.valueOf((String) row[10]));
        
        // Convert category timestamps
        if (row[11] != null) {
            category.setCreatedAt(((java.sql.Timestamp) row[11]).toLocalDateTime());
        }
        if (row[12] != null) {
            category.setUpdatedAt(((java.sql.Timestamp) row[12]).toLocalDateTime());
        }
        
        product.setCategory(category);
        
        return product;
    }
}
