package com.omnicharge.api_gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtUtil in api-gateway.
 *
 * Covers:
 *  - validateToken() — valid token returns true
 *  - validateToken() — expired token returns false
 *  - validateToken() — tampered token returns false
 *  - extractUsername() — correctly parses subject
 *  - extractRole()     — correctly parses role claim
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("api-gateway JwtUtil Unit Tests")
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    // Must be at least 256 bits (32 chars) for HS256
    private static final String SECRET = "omnicharge-super-secret-jwt-key-32ch";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Key signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    private String buildToken(String username, String role, long expiresInMs) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiresInMs))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── validateToken() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken() - valid token returns true")
    void validateToken_valid_returnsTrue() {
        String token = buildToken("alice", "ROLE_USER", 60_000);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken() - expired token returns false")
    void validateToken_expired_returnsFalse() {
        String token = buildToken("alice", "ROLE_USER", -1_000); // already expired
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken() - tampered token returns false")
    void validateToken_tampered_returnsFalse() {
        String token = buildToken("alice", "ROLE_USER", 60_000);
        // Flip last character to simulate tampering
        String tampered = token.substring(0, token.length() - 1) + "X";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken() - garbage string returns false")
    void validateToken_garbage_returnsFalse() {
        assertThat(jwtUtil.validateToken("not.a.jwt")).isFalse();
    }

    // ── extractUsername() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("extractUsername() - returns subject from token")
    void extractUsername_returnsSubject() {
        String token = buildToken("bob", "ROLE_ADMIN", 60_000);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("bob");
    }

    // ── extractRole() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractRole() - returns role claim from token")
    void extractRole_returnsRole() {
        String token = buildToken("alice", "ROLE_USER", 60_000);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("extractRole() - returns ROLE_ADMIN for admin token")
    void extractRole_adminRole() {
        String token = buildToken("admin", "ROLE_ADMIN", 60_000);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_ADMIN");
    }
}