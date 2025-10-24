package com.webmini.miniweb.catalog.category.repo;

import com.webmini.miniweb.catalog.category.entity.Category;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {
    
    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public Category save(Category category) {
        if (category.getId() == null) {
            // Insert
            String sql = """
                INSERT INTO categories (name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?)
            """;
            em.createNativeQuery(sql)
                .setParameter(1, category.getName())
                .setParameter(2, category.getStatus().name())
                .setParameter(3, category.getCreatedAt() != null ? category.getCreatedAt() : LocalDateTime.now())
                .setParameter(4, category.getUpdatedAt())
                .executeUpdate();
            
            // Get generated ID
            Long id = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
            category.setId(id);
            return category;
        } else {
            // Update
            String sql = """
                UPDATE categories
                SET name = ?, status = ?, updated_at = ?
                WHERE id = ?
            """;
            em.createNativeQuery(sql)
                .setParameter(1, category.getName())
                .setParameter(2, category.getStatus().name())
                .setParameter(3, category.getUpdatedAt())
                .setParameter(4, category.getId())
                .executeUpdate();
            return category;
        }
    }

    @Override
    public Optional<Category> findById(Long id) {
        String sql = """
            SELECT id, name, status, created_at, updated_at
            FROM categories
            WHERE id = ?
        """;
        
        try {
            Object[] result = (Object[]) em.createNativeQuery(sql)
                .setParameter(1, id)
                .getSingleResult();
            
            return Optional.of(mapToCategory(result));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        String sql = "SELECT COUNT(*) FROM categories WHERE LOWER(name) = LOWER(?)";
        Long count = ((Number) em.createNativeQuery(sql)
            .setParameter(1, name)
            .getSingleResult()).longValue();
        return count > 0;
    }

    @Override
    public Optional<Category> findByNameIgnoreCase(String name) {
        String sql = """
            SELECT id, name, status, created_at, updated_at
            FROM categories
            WHERE LOWER(name) = LOWER(?)
        """;
        
        try {
            Object[] result = (Object[]) em.createNativeQuery(sql)
                .setParameter(1, name)
                .getSingleResult();
            
            return Optional.of(mapToCategory(result));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM categories WHERE id = ?";
        Long count = ((Number) em.createNativeQuery(sql)
            .setParameter(1, id)
            .getSingleResult()).longValue();
        return count > 0;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        String sql = "DELETE FROM categories WHERE id = ?";
        em.createNativeQuery(sql)
            .setParameter(1, id)
            .executeUpdate();
    }

    @Override
    public Page<Category> search(String q, String status, Pageable pageable) {

        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        int paramIndex = 1;
        
        if (q != null && !q.isBlank()) {
            whereClause.append(" AND LOWER(name) LIKE ?").append(paramIndex);
            params.add("%" + q.toLowerCase() + "%");
            paramIndex++;
        }
        
        if (status != null && !status.isBlank()) {
            whereClause.append(" AND status = ?").append(paramIndex);
            params.add(status);
            paramIndex++;
        }

        // Return
        String countSql = "SELECT COUNT(*) FROM categories" + whereClause;
        var countQuery = em.createNativeQuery(countSql);
        for (int i = 0; i < params.size(); i++) {
            countQuery.setParameter(i + 1, params.get(i));
        }
        Long total = ((Number) countQuery.getSingleResult()).longValue();
        
        // Get data with pagination
        String dataSql = """
            SELECT id, name, status, created_at, updated_at
            FROM categories
        """ + whereClause + " ORDER BY " + buildOrderBy(pageable) + " LIMIT ?" + paramIndex + " OFFSET ?" + (paramIndex + 1);
        
        var dataQuery = em.createNativeQuery(dataSql);
        for (int i = 0; i < params.size(); i++) {
            dataQuery.setParameter(i + 1, params.get(i));
        }
        dataQuery.setParameter(paramIndex, pageable.getPageSize());
        dataQuery.setParameter(paramIndex + 1, (int) pageable.getOffset());
        
        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<Category> categories = rows.stream().map(this::mapToCategory).toList();
        
        return new PageImpl<>(categories, pageable, total);
    }

    private String buildOrderBy(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            StringBuilder orderBy = new StringBuilder();
            pageable.getSort().forEach(order -> {
                if (!orderBy.isEmpty()) orderBy.append(", ");
                // Map Java property names to database column names
                String columnName = mapPropertyToColumn(order.getProperty());
                orderBy.append(columnName).append(" ").append(order.getDirection().name());
            });
            return orderBy.toString();
        }
        return "id DESC";
    }
    
    private String mapPropertyToColumn(String property) {
        return switch (property) {
            case "createdAt" -> "created_at";
            case "updatedAt" -> "updated_at";
            default -> property; // id, name, status remain the same
        };
    }

    private Category mapToCategory(Object[] row) {
        Category category = new Category();
        category.setId(((Number) row[0]).longValue());
        category.setName((String) row[1]);
        category.setStatus(Category.CategoryStatus.valueOf((String) row[2]));
        
        // Convert java.sql.Timestamp to LocalDateTime
        if (row[3] != null) {
            category.setCreatedAt(((java.sql.Timestamp) row[3]).toLocalDateTime());
        }
        if (row[4] != null) {
            category.setUpdatedAt(((java.sql.Timestamp) row[4]).toLocalDateTime());
        }
        
        return category;
    }
}
