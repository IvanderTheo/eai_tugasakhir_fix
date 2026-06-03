package com.example.adminservice.controller;

import com.example.adminservice.dto.AuthTokenResponse;
import com.example.adminservice.dto.LoginRequest;
import com.example.adminservice.dto.RefreshTokenRequest;
import com.example.adminservice.dto.RegisterRequest;
import com.example.adminservice.entity.User;
import com.example.adminservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "Authentication", description = "Register, login, and refresh JWT (JSON body)")
public class AuthController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Register user", description = "Create a new user account. Role: ADMIN, DOCTOR, PHARMACIST, STAFF, USER")
    @ApiResponse(responseCode = "201", description = "User registered")
    @PostMapping(value = "/register", 
    consumes=MediaType.APPLICATION_JSON_VALUE,
    produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.info("=== REGISTER HIT ===");
            log.info("username={}", request.getUsername());
            userService.registerUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getNama(),
                    request.getEmail(),
                    request.getRole() != null ? request.getRole() : "USER");
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("username", request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @Operation(summary = "Login", description = "Returns JWT token. Use token in Swagger **Authorize** for other endpoints.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AuthTokenResponse.class)))
    @PostMapping(value = "/login", 
    consumes=MediaType.APPLICATION_JSON_VALUE,
    produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = userService.authenticateUser(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(AuthTokenResponse.builder()
                    .token(token)
                    .username(request.getUsername())
                    .message("Login successful")
                    .build());
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @Operation(summary = "Refresh token")
    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String token = userService.refreshToken(request.getUsername());
            return ResponseEntity.ok(AuthTokenResponse.builder()
                    .token(token)
                    .username(request.getUsername())
                    .message("Token refreshed successfully")
                    .build());
        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @Operation(summary = "Get user profile", description = "Returns current logged-in user information including ID")
    @ApiResponse(responseCode = "200", description = "User profile retrieved")
    @GetMapping(value = "/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("nama", user.getNama());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("isActive", user.getIsActive());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching profile: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
