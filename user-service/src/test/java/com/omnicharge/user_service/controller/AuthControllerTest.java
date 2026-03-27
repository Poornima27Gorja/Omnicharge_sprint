package com.omnicharge.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.user_service.dto.*;
import com.omnicharge.user_service.exception.GlobalExceptionHandler;
import com.omnicharge.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController standalone MockMvc test.
 *
 * Key points: - standaloneSetup() = zero Spring context, zero Security, no 401
 * ever - GlobalExceptionHandler registered manually so RuntimeException → 400 -
 * No DB, no RabbitMQ, no Eureka, services do NOT need to be running -
 * UserService is fully mocked with Mockito
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Web Layer Tests")
class AuthControllerTest {

	@InjectMocks
	private AuthController authController;

	@Mock
	private UserService userService;

	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(authController)
				// MUST register GlobalExceptionHandler so RuntimeException
				// is caught and returns 400, not a raw ServletException Error
				.setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	// ── POST /api/auth/register ───────────────────────────────────────────────

	@Test
	@DisplayName("POST /api/auth/register - 200 OK on successful registration")
	void register_returns200() throws Exception {
		RegisterRequest req = new RegisterRequest();
		req.setUsername("alice");
		req.setEmail("alice@test.com");
		req.setPassword("pass123");
		req.setFullName("Alice");
		req.setPhone("9876543210");

		AuthResponse resp = new AuthResponse("jwt.token", "alice", "ROLE_USER", "User registered successfully");

		when(userService.register(any(RegisterRequest.class))).thenReturn(resp);

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("jwt.token")).andExpect(jsonPath("$.username").value("alice"))
				.andExpect(jsonPath("$.role").value("ROLE_USER"))
				.andExpect(jsonPath("$.message").value("User registered successfully"));
	}

	@Test
	@DisplayName("POST /api/auth/register - duplicate username returns 400")
	void register_duplicateUsername_returns400() throws Exception {
		RegisterRequest req = new RegisterRequest();
		req.setUsername("alice");
		req.setEmail("alice@test.com");
		req.setPassword("pass123");
		req.setFullName("Alice");
		req.setPhone("9876543210");

		when(userService.register(any(RegisterRequest.class)))
				.thenThrow(new RuntimeException("Username already exists: alice"));

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Username already exists: alice"));
	}

	// ── POST /api/auth/login 

	@Test
	@DisplayName("POST /api/auth/login - 200 OK with JWT token")
	void login_returns200() throws Exception {
		LoginRequest req = new LoginRequest();
		req.setUsername("alice");
		req.setPassword("pass123");

		AuthResponse resp = new AuthResponse("jwt.login.token", "alice", "ROLE_USER", "Login successful");

		when(userService.login(any(LoginRequest.class))).thenReturn(resp);

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("jwt.login.token"))
				.andExpect(jsonPath("$.message").value("Login successful"));
	}

	@Test
	@DisplayName("POST /api/auth/login - bad credentials returns 400")
	void login_badCredentials_returns400() throws Exception {
		LoginRequest req = new LoginRequest();
		req.setUsername("alice");
		req.setPassword("wrongpass");

		when(userService.login(any(LoginRequest.class)))
				.thenThrow(new RuntimeException("Invalid username or password"));

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid username or password"));
	}

	// ── POST /api/auth/register-admin 

	@Test
	@DisplayName("POST /api/auth/register-admin - 200 OK with valid secret key")
	void registerAdmin_returns200() throws Exception {
		AdminRegisterRequest req = new AdminRegisterRequest();
		req.setUsername("adminuser");
		req.setEmail("admin@test.com");
		req.setPassword("adminpass");
		req.setFullName("Admin User");
		req.setPhone("9000000001");
		req.setAdminSecretKey("omniCharge008");

		AuthResponse resp = new AuthResponse("jwt.admin.token", "adminuser", "ROLE_ADMIN",
				"Admin registered successfully");

		when(userService.registerAdmin(any(AdminRegisterRequest.class))).thenReturn(resp);

		mockMvc.perform(post("/api/auth/register-admin").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
				.andExpect(jsonPath("$.token").value("jwt.admin.token"));
	}

	@Test
	@DisplayName("POST /api/auth/register-admin - wrong secret key returns 400")
	void registerAdmin_wrongKey_returns400() throws Exception {
		AdminRegisterRequest req = new AdminRegisterRequest();
		req.setUsername("hacker");
		req.setEmail("hacker@test.com");
		req.setPassword("hackpass");
		req.setFullName("Hacker");
		req.setPhone("9000000002");
		req.setAdminSecretKey("wrong_key");

		when(userService.registerAdmin(any(AdminRegisterRequest.class)))
				.thenThrow(new RuntimeException("Invalid admin secret key"));

		mockMvc.perform(post("/api/auth/register-admin").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid admin secret key"));
	}
}