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
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

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

	// Regular user registration - always ROLE_USER
	@Override
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByUsername(request.getUsername())) {
			throw new RuntimeException("Username already exists: " + request.getUsername());
		}
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new RuntimeException("Email already exists: " + request.getEmail());
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setFullName(request.getFullName());
		user.setPhone(request.getPhone());
		user.setActive(true);
		user.setRole(Role.ROLE_USER);

		userRepository.save(user);

		String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
		return new AuthResponse(token, user.getUsername(), user.getRole().name(), "User registered successfully");
	}

	// Admin registration - requires correct secret key
	@Override
	public AuthResponse registerAdmin(AdminRegisterRequest request) {
		// Validate secret key first - reject immediately if wrong
		if (request.getAdminSecretKey() == null || !request.getAdminSecretKey().equals(adminSecretKey)) {
			throw new RuntimeException("Invalid admin secret key");
		}

		if (userRepository.existsByUsername(request.getUsername())) {
			throw new RuntimeException("Username already exists: " + request.getUsername());
		}
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new RuntimeException("Email already exists: " + request.getEmail());
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setFullName(request.getFullName());
		user.setPhone(request.getPhone());
		user.setActive(true);
		user.setRole(Role.ROLE_ADMIN);

		userRepository.save(user);

		String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
		return new AuthResponse(token, user.getUsername(), user.getRole().name(), "Admin registered successfully");
	}

	@Override
	public AuthResponse login(LoginRequest request) {
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
		} catch (AuthenticationException ex) {
			throw new RuntimeException("Invalid username or password");
		}

		User user = userRepository.findByUsername(request.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
		return new AuthResponse(token, user.getUsername(), user.getRole().name(), "Login successful");
	}

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
	
	@Override
	public String changePassword(String username, ChangePasswordRequest request) {
	    User user = userRepository.findByUsername(username)
	            .orElseThrow(() -> new RuntimeException("User not found: " + username));

	    // Check current password is correct
	    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
	        throw new RuntimeException("Current password is incorrect");
	    }

	    // Check new password and confirm password match
	    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
	        throw new RuntimeException("New password and confirm password do not match");
	    }

	    // Check new password is not same as current password
	    if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
	        throw new RuntimeException("New password must be different from current password");
	    }

	    // Check minimum password length
	    if (request.getNewPassword().length() < 6) {
	        throw new RuntimeException("New password must be at least 6 characters");
	    }

	    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
	    userRepository.save(user);

	    return "Password changed successfully";
	}
}