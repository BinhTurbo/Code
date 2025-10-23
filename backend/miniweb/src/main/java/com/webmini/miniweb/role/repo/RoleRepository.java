package com.webmini.miniweb.role.repo;

import com.webmini.miniweb.role.entity.Role;
import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findById(Long id);
    Optional<Role> findByCode(String code);
    Role save(Role role);
}