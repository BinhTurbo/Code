package com.webmini.miniweb.user.controller;

import com.webmini.miniweb.auth.dto.AuthDtos;
import com.webmini.miniweb.auth.entity.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    @GetMapping("/me")
    public AuthDtos.MeResponse me(Authentication authentication) {
        UserPrincipal p = (UserPrincipal) authentication.getPrincipal();
        String role = p.getAuthorities().stream().findFirst().map(GrantedAuthority::getAuthority).orElse("");
        return new AuthDtos.MeResponse(p.getId(), p.getUsername(), p.getFullName(), role);
    }
}