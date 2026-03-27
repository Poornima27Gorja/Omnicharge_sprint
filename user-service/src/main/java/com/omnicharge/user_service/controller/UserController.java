package com.omnicharge.user_service.controller;

import com.omnicharge.user_service.dto.ChangePasswordRequest;
import com.omnicharge.user_service.dto.UserDto;
import com.omnicharge.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User profile and admin management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserService userService;

    // ─── Available to any authenticated user ────────────────────────────────────

    @GetMapping("/profile")
    @Operation(summary = "Get own profile", description = "Returns the profile of the currently authenticated user.")
    public ResponseEntity<UserDto> getProfile(Authentication authentication) {
        UserDto userDto = userService.getUserProfile(authentication.getName());
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change own password", description = "Requires current password for verification.")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request) {
        String message = userService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    // ─── Admin-only endpoints ────────────────────────────────────────────────────

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get all users [ADMIN ONLY]",
        description = "Returns a list of every registered user. Requires ROLE_ADMIN token."
    )
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/promote/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Promote user to admin [ADMIN ONLY]",
        description = "Upgrades an existing ROLE_USER account to ROLE_ADMIN. Requires ROLE_ADMIN token."
    )
    public ResponseEntity<UserDto> promoteToAdmin(@PathVariable Long userId) {
        UserDto userDto = userService.promoteToAdmin(userId);
        return ResponseEntity.ok(userDto);
    }
}