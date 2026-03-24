package com.omnicharge.user_service.controller;

import com.omnicharge.user_service.dto.*;
import com.omnicharge.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	private UserService userService;

	// Register regular user - no secret key, always ROLE_USER
	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
		AuthResponse response = userService.register(request);
		return ResponseEntity.ok(response);
	}

	// Register admin - requires valid adminSecretKey in request body
	@PostMapping("/register-admin")
	public ResponseEntity<AuthResponse> registerAdmin(@RequestBody AdminRegisterRequest request) {
		AuthResponse response = userService.registerAdmin(request);
		return ResponseEntity.ok(response);
	}

	// Login - same endpoint for both user and admin
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
		AuthResponse response = userService.login(request);
		return ResponseEntity.ok(response);
	}
}