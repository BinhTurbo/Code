package com.webmini.miniweb.auth.service;

import com.webmini.miniweb.auth.dto.AuthDtos;
import com.webmini.miniweb.auth.entity.JwtProperties;
import com.webmini.miniweb.auth.entity.UserPrincipal;
import com.webmini.miniweb.common.ConflictException;
import com.webmini.miniweb.common.ValidationException;
import com.webmini.miniweb.role.entity.Role;
import com.webmini.miniweb.role.repo.RoleRepository;
import com.webmini.miniweb.user.dto.RegisterDtos;
import com.webmini.miniweb.user.entity.User;
import com.webmini.miniweb.user.repo.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final CustomUserDetailsService uds;
    private final JwtProperties props;
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public AuthService(AuthenticationManager am, JwtService jwt, CustomUserDetailsService uds,
                       JwtProperties props, UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.authManager = am;
        this.jwt = jwt;
        this.uds = uds;
        this.props = props;
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    public AuthDtos.TokenResponse login(String username, String rawPassword) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, rawPassword)
        );
        UserPrincipal p = (UserPrincipal) auth.getPrincipal();
        String access = jwt.generateAccessToken(p);
        String refresh = jwt.generateRefreshToken(p);
        long accessTtlSec = props.getAccessTtl().toSeconds();
        return new AuthDtos.TokenResponse("Bearer", access, accessTtlSec, refresh);
    }

    public AuthDtos.TokenResponse refresh(String refreshToken) {
        if (!jwt.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        String username = jwt.getUsername(refreshToken);
        UserPrincipal p = (UserPrincipal) uds.loadUserByUsername(username);
        String access = jwt.generateAccessToken(p);
        String refresh = jwt.generateRefreshToken(p);
        long accessTtlSec = props.getAccessTtl().toSeconds();
        return new AuthDtos.TokenResponse("Bearer", access, accessTtlSec, refresh);
    }

    public AuthDtos.TokenResponse register(RegisterDtos.RegisterRequest req) {
        validateRegistrationRequest(req);
        if (users.existsByUsername(req.username())) {
            throw new ConflictException("Tên đăng nhập '" + req.username() + "' đã được sử dụng");
        }
        Role userRole = roles.findByCode("USER")
                .orElseThrow(() -> new IllegalStateException("USER role not seeded"));
        User u = new User();
        u.setUsername(req.username().trim());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setFullName(req.fullName().trim());
        u.setRole(userRole);
        u.setStatus(User.Status.ACTIVE);

        try {
            users.save(u);
        } catch (Exception e) {
            throw new ValidationException("Không thể tạo tài khoản. Vui lòng thử lại.");
        }

        // Auto-login
        UserPrincipal p = new UserPrincipal(u);
        String access = jwt.generateAccessToken(p);
        String refresh = jwt.generateRefreshToken(p);
        long accessTtlSec = props.getAccessTtl().toSeconds();
        return new AuthDtos.TokenResponse("Bearer", access, accessTtlSec, refresh);
    }

    private void validateRegistrationRequest(RegisterDtos.RegisterRequest req) {
        String username = req.username().trim();
        String fullName = req.fullName().trim();
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            throw new ValidationException("Tên đăng nhập chỉ được chứa chữ cái, số, dấu gạch dưới (_) và gạch ngang (-)");
        }
        if (fullName.split("\\s+").length < 2) {
            throw new ValidationException("Vui lòng nhập đầy đủ họ và tên (ít nhất 2 từ)");
        }
        String password = req.password();
        if (!password.matches(".*[A-Z].*")) {
            throw new ValidationException("Mật khẩu phải có ít nhất 1 chữ in hoa");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new ValidationException("Mật khẩu phải có ít nhất 1 chữ thường");
        }
        if (!password.matches(".*\\d.*")) {
            throw new ValidationException("Mật khẩu phải có ít nhất 1 chữ số");
        }
        if (username.length() > 100) {
            throw new ValidationException("Tên đăng nhập không được vượt quá 100 ký tự");
        }
        if (fullName.length() > 150) {
            throw new ValidationException("Họ tên không được vượt quá 150 ký tự");
        }
        if (password.length() > 72) {
            throw new ValidationException("Mật khẩu không được vượt quá 72 ký tự");
        }
    }
}