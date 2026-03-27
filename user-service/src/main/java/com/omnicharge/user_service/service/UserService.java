package com.omnicharge.user_service.service;

import com.omnicharge.user_service.dto.*;

import java.util.List;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse registerAdmin(AdminRegisterRequest request);

    // Shared login — returns token for both ROLE_USER and ROLE_ADMIN
    AuthResponse login(LoginRequest request);

    // Admin-portal login — rejects the request if the account is not ROLE_ADMIN
    AuthResponse adminLogin(LoginRequest request);

    UserDto getUserProfile(String username);

    List<UserDto> getAllUsers();

    UserDto promoteToAdmin(Long userId);

    String changePassword(String username, ChangePasswordRequest request);
}