package com.omnicharge.user_service.service;

import com.omnicharge.user_service.dto.*;
import com.omnicharge.user_service.entity.Role;
import com.omnicharge.user_service.entity.User;
import com.omnicharge.user_service.repository.UserRepository;
import com.omnicharge.user_service.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    // Strict email regex: requires valid domain and TLD (at least 2 chars after the last dot)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Value("${app.admin.secret-key}")
    private String adminSecretKey;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new RuntimeException(
                "Invalid email address '" + email + "'. " +
                "Please use a valid format like user@example.com"
            );
        }
    }

    // ─── Register regular user ───────────────────────────────────────────────────

    @Override
    public AuthResponse register(RegisterRequest request) {
        validateEmail(request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setActive(true);
        user.setRole(Role.ROLE_USER);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), "User registered successfully");
    }

    // ─── Register admin ──────────────────────────────────────────────────────────

    @Override
    public AuthResponse registerAdmin(AdminRegisterRequest request) {
        if (request.getAdminSecretKey() == null || !request.getAdminSecretKey().equals(adminSecretKey)) {
            throw new RuntimeException("Invalid admin secret key");
        }

        validateEmail(request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setActive(true);
        user.setRole(Role.ROLE_ADMIN);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), "Admin registered successfully");
    }

    // ─── Standard login — ROLE_USER only ────────────────────────────────────────

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new RuntimeException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated. Please contact support.");
        }

        // ✅ FIX: This endpoint is for regular users only — reject admin accounts
        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new RuntimeException(
                "Invalid user"
            );
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), "Login successful");
    }

    // ─── Admin-portal login — REJECTS non-admins ────────────────────────────────

    @Override
    public AuthResponse adminLogin(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new RuntimeException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated. Please contact support.");
        }

        // This is the admin portal — reject regular users
        if (user.getRole() != Role.ROLE_ADMIN) {
            throw new RuntimeException(
                "Access denied. This portal is for administrators only. " +
                "Invalid Admin"
            );
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), "Admin login successful");
    }

    // ─── Profile ─────────────────────────────────────────────────────────────────

    @Override
    public UserDto getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return mapToDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public UserDto promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new RuntimeException("User is already an admin");
        }

        user.setRole(Role.ROLE_ADMIN);
        userRepository.save(user);
        return mapToDto(user);
    }

    @Override
    public String changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("New password must be different from current password");
        }
        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return "Password changed successfully";
    }

    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().name());
        dto.setActive(user.isActive());
        return dto;
    }
}