package com.omnicharge.user_service.security;

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
 * Unit tests for JwtUtil in user-service.
 *
 * Covers:
 *  - generateToken() — produces a non-blank JWT
 *  - validateToken() — valid, expired, tampered
 *  - extractUsername() / extractRole()
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("user-service JwtUtil Unit Tests")
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private static final String SECRET     = "omnicharge-super-secret-jwt-key-32ch";
    private static final long   EXPIRATION = 3_600_000L; // 1 hour

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret",     SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    /** Builds a token that is already expired. */
    private String expiredToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date(System.currentTimeMillis() - 10_000))
                .setExpiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // generateToken()

    @Test
    @DisplayName("generateToken() - returns non-blank token string")
    void generateToken_returnsNonBlank() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER");
        assertThat(token).isNotBlank();
        // A JWT has exactly 3 dot-separated parts
        assertThat(token.split("\\.")).hasSize(3);
    }

    // validateToken() 

    @Test
    @DisplayName("validateToken() - freshly generated token is valid")
    void validateToken_freshToken_isTrue() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken() - expired token returns false")
    void validateToken_expiredToken_isFalse() {
        assertThat(jwtUtil.validateToken(expiredToken("alice", "ROLE_USER"))).isFalse();
    }

    @Test
    @DisplayName("validateToken() - tampered signature returns false")
    void validateToken_tampered_isFalse() {
        String token = jwtUtil.generateToken("alice", "ROLE_USER");
        String tampered = token.substring(0, token.length() - 1) + "Z";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken() - garbage input returns false")
    void validateToken_garbage_isFalse() {
        assertThat(jwtUtil.validateToken("this.is.garbage")).isFalse();
    }

    // extractUsername() 

    @Test
    @DisplayName("extractUsername() - returns subject set during generation")
    void extractUsername_returnsSubject() {
        String token = jwtUtil.generateToken("charlie", "ROLE_USER");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("charlie");
    }

    // extractRole() 

    @Test
    @DisplayName("extractRole() - returns role claim set during generation")
    void extractRole_returnsRole() {
        String token = jwtUtil.generateToken("adminUser", "ROLE_ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("extractRole() - ROLE_USER is preserved")
    void extractRole_userRole() {
        String token = jwtUtil.generateToken("regularUser", "ROLE_USER");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_USER");
    }
}