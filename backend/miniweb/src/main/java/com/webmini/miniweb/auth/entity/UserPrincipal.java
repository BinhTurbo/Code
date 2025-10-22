package com.webmini.miniweb.auth.entity;

import com.webmini.miniweb.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class UserPrincipal implements UserDetails {
    private final User user;

    public UserPrincipal(User user) { this.user = user; }
    public Long getId() { return user.getId(); }
    public String getFullName() { return user.getFullName(); }
    public String getRoleCode() { return user.getRole().getCode(); }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = "ROLE_" + user.getRole().getCode();
        return java.util.List.of(new SimpleGrantedAuthority(role));
    }
    @Override public String getPassword() { return user.getPasswordHash(); }
    @Override public String getUsername() { return user.getUsername(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return user.isEnabled(); }
}