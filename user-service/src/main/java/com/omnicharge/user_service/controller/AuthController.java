package com.omnicharge.user_service.controller;

import com.omnicharge.user_service.dto.*;
import com.omnicharge.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * Register a new regular user.
     * Role is always ROLE_USER — no secret key needed.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a ROLE_USER account. No secret key needed.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Register a new admin user.
     * Requires a valid adminSecretKey in the request body.
     */
    @PostMapping("/register-admin")
    @Operation(
        summary = "Register a new admin",
        description = "Creates a ROLE_ADMIN account. Requires the adminSecretKey configured on the server."
    )
    public ResponseEntity<AuthResponse> registerAdmin(@Valid @RequestBody AdminRegisterRequest request) {
        AuthResponse response = userService.registerAdmin(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Standard login — accepts both ROLE_USER and ROLE_ADMIN accounts.
     * Returns a JWT token containing the user's role.
     * The front-end should route based on the 'role' field in the response.
     */
    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Login for regular users. Returns JWT with role=ROLE_USER. " +
                      "Admins can also use this, but the token returned will be usable " +
                      "at admin endpoints only if the role is ROLE_ADMIN."
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin-portal login — REJECTS the request if the account is not ROLE_ADMIN.
     *
     * Real-world behaviour: admin portals have a dedicated login URL.
     * A regular user trying to log in here gets a clear "administrators only" error
     * instead of silently receiving a user token that would be rejected downstream.
     */
    @PostMapping("/admin/login")
    @Operation(
        summary = "Admin portal login",
        description = "Login exclusively for ROLE_ADMIN accounts. " +
                      "Regular users will receive HTTP 400 with an 'administrators only' message. " +
                      "Use the token from this endpoint to access all admin-protected endpoints."
    )
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.adminLogin(request);
        return ResponseEntity.ok(response);
    }
}