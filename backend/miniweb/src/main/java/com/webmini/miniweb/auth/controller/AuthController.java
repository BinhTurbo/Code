package com.webmini.miniweb.auth.controller;

import com.webmini.miniweb.auth.dto.AuthDtos;
import com.webmini.miniweb.auth.service.AuthService;
import com.webmini.miniweb.user.dto.RegisterDtos;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public AuthDtos.TokenResponse login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return auth.login(req.username(), req.password());
    }

    @PostMapping("/refresh")
    public AuthDtos.TokenResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest req) {
        return auth.refresh(req.refreshToken());
    }

    @PostMapping("/register")
    public AuthDtos.TokenResponse register(@Valid @RequestBody RegisterDtos.RegisterRequest req) {
        return auth.register(req);
    }
}
