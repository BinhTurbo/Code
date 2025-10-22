package com.webmini.miniweb.auth.service;

import com.webmini.miniweb.auth.entity.JwtProperties;
import com.webmini.miniweb.auth.entity.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtService {
    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                props.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(UserPrincipal p) {
        return buildToken(p, "access", props.getAccessTtl());
    }

    public String generateRefreshToken(UserPrincipal p) {
        return buildToken(p, "refresh", props.getRefreshTtl());
    }

    private String buildToken(UserPrincipal p, String type, Duration ttl) {
        long now = System.currentTimeMillis();
        Date iat = new Date(now);
        Date exp = new Date(now + ttl.toMillis());

        return Jwts.builder()
                .setIssuer(props.getIssuer())
                .setSubject(p.getUsername())
                .claim("uid", p.getId())
                .claim("roles", p.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .claim("typ", type)
                .setIssuedAt(iat)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .requireIssuer(props.getIssuer())
                .clockSkewSeconds(60)
                .verifyWith(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parse(token).get("typ", String.class));
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }
}