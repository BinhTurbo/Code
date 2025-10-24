package com.webmini.miniweb.role.repo;

import com.webmini.miniweb.role.entity.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class RoleRepositoryImpl implements RoleRepository {
    
    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Role> findById(Long id) {
        String sql = """
            SELECT id, code, name
            FROM roles
            WHERE id = ?
        """;
        
        try {
            Object[] result = (Object[]) em.createNativeQuery(sql)
                .setParameter(1, id)
                .getSingleResult();
            
            return Optional.of(mapToRole(result));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Role> findByCode(String code) {
        String sql = """
            SELECT id, code, name
            FROM roles
            WHERE code = ?
        """;
        
        try {
            Object[] result = (Object[]) em.createNativeQuery(sql)
                .setParameter(1, code)
                .getSingleResult();
            
            return Optional.of(mapToRole(result));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public Role save(Role role) {
        if (role.getId() == null) {
            // Insert
            String sql = """
                INSERT INTO roles (code, name)
                VALUES (?, ?)
            """;
            em.createNativeQuery(sql)
                .setParameter(1, role.getCode())
                .setParameter(2, role.getName())
                .executeUpdate();
            
            Long id = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
            role.setId(id);
            return role;
        } else {
            // Update
            String sql = """
                UPDATE roles
                SET code = ?, name = ?
                WHERE id = ?
            """;
            em.createNativeQuery(sql)
                .setParameter(1, role.getCode())
                .setParameter(2, role.getName())
                .setParameter(3, role.getId())
                .executeUpdate();
            return role;
        }
    }

    private Role mapToRole(Object[] row) {
        Role role = new Role();
        role.setId(((Number) row[0]).longValue());
        role.setCode((String) row[1]);
        role.setName((String) row[2]);
        return role;
    }
}
