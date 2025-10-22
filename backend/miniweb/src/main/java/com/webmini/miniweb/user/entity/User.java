package com.webmini.miniweb.user.entity;

import com.webmini.miniweb.role.entity.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="users")
@Getter
@Setter
public class User {
    public enum Status { ACTIVE, INACTIVE }

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=100)
    private String username;

    @Column(name="password_hash", nullable=false, length=255)
    private String passwordHash;

    @Column(name="full_name", length=150)
    private String fullName;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="role_id", nullable=false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=10)
    private Status status = Status.ACTIVE;

    @Column(name="created_at", nullable=false, updatable=false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    @Transient
    public boolean isEnabled() { return status == Status.ACTIVE; }
}