package com.webmini.miniweb.user.repo;

import com.webmini.miniweb.role.entity.Role;
import com.webmini.miniweb.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {
    
    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public User save(User user) {
        if (user.getId() == null) {
            // Insert
            String sql = """
                INSERT INTO users (username, password_hash, full_name, role_id, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
            """;
            em.createNativeQuery(sql)
                .setParameter(1, user.getUsername())
                .setParameter(2, user.getPasswordHash())
                .setParameter(3, user.getFullName())
                .setParameter(4, user.getRole().getId())
                .setParameter(5, user.getStatus().name())
                .setParameter(6, user.getCreatedAt())
                .executeUpdate();
            
            // Get generated ID
            Long id = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
            user.setId(id);
            return user;
        } else {
            // Update
            String sql = """
                UPDATE users
                SET username = ?, password_hash = ?, full_name = ?, role_id = ?, status = ?
                WHERE id = ?
            """;
            em.createNativeQuery(sql)
                .setParameter(1, user.getUsername())
                .setParameter(2, user.getPasswordHash())
                .setParameter(3, user.getFullName())
                .setParameter(4, user.getRole().getId())
                .setParameter(5, user.getStatus().name())
                .setParameter(6, user.getId())
                .executeUpdate();
            return user;
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = """
            SELECT u.id, u.username, u.password_hash, u.full_name, u.status, u.created_at,
                   r.id as role_id, r.code as role_code, r.name as role_name
            FROM users u
            INNER JOIN roles r ON u.role_id = r.id
            WHERE u.id = ?
        """;
        
        try {
            Object[] result = (Object[]) em.createNativeQuery(sql)
                .setParameter(1, id)
                .getSingleResult();
            
            return Optional.of(mapToUser(result));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = """
            SELECT u.id, u.username, u.password_hash, u.full_name, u.status, u.created_at,
                   r.id as role_id, r.code as role_code, r.name as role_name
            FROM users u
            INNER JOIN roles r ON u.role_id = r.id
            WHERE u.username = ?
        """;
        
        try {
            Object[] result = (Object[]) em.createNativeQuery(sql)
                .setParameter(1, username)
                .getSingleResult();
            
            return Optional.of(mapToUser(result));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Long count = ((Number) em.createNativeQuery(sql)
            .setParameter(1, username)
            .getSingleResult()).longValue();
        return count > 0;
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        Long count = ((Number) em.createNativeQuery(sql)
            .setParameter(1, id)
            .getSingleResult()).longValue();
        return count > 0;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        em.createNativeQuery(sql)
            .setParameter(1, id)
            .executeUpdate();
    }

    private User mapToUser(Object[] row) {
        User user = new User();
        user.setId(((Number) row[0]).longValue());
        user.setUsername((String) row[1]);
        user.setPasswordHash((String) row[2]);
        user.setFullName((String) row[3]);
        user.setStatus(User.Status.valueOf((String) row[4]));
        
        // Convert java.sql.Timestamp to LocalDateTime
        if (row[5] != null) {
            user.setCreatedAt(((java.sql.Timestamp) row[5]).toLocalDateTime());
        }
        
        Role role = new Role();
        role.setId(((Number) row[6]).longValue());
        role.setCode((String) row[7]);
        role.setName((String) row[8]);
        user.setRole(role);
        
        return user;
    }
}
