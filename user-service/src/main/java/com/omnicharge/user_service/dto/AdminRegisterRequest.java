package com.omnicharge.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AdminRegisterRequest {

	@NotBlank(message = "Username is required")
	@Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
	@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
	private String username;

	@NotBlank(message = "Email is required")
	@Email(regexp = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$", message = "Please provide a valid email address (e.g. admin@omnicharge.com)")
	private String email;

	@NotBlank(message = "Password is required")
	@Size(min = 8, message = "Admin password must be at least 8 characters")
	private String password;

	@NotBlank(message = "Full name is required")
	@Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
	private String fullName;

	@NotBlank(message = "Phone number is required")
	@Pattern(regexp = "^[6-9]\\d{9}$", message = "Please provide a valid 10-digit Indian mobile number")
	private String phone;

	@NotBlank(message = "Admin secret key is required")
	private String adminSecretKey;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getAdminSecretKey() {
		return adminSecretKey;
	}

	public void setAdminSecretKey(String adminSecretKey) {
		this.adminSecretKey = adminSecretKey;
	}
}