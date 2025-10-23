package com.webmini.miniweb.user.repo;

import com.webmini.miniweb.user.entity.User;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsById(Long id);
    void deleteById(Long id);
}