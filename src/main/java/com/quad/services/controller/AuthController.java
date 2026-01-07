package com.quad.services.controller;

import com.quad.services.dto.AuthResponse;
import com.quad.services.dto.LoginRequest;
import com.quad.services.dto.SignupRequest;
import com.quad.services.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication Controller
 *
 * Handles user authentication operations including signup, login, and health checks.
 * All endpoints in this controller are publicly accessible (no JWT required).
 *
 * @author QUAD Platform
 * @since 1.0.0
 */
@RestController
@RequestMapping("${api.version.prefix:/v1}/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")  // TODO: Configure properly for production
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Create new organization and user account",
            description = "Register a new organization with first admin user. Creates both organization and user records. " +
                    "Returns JWT token for immediate authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Account created successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or email already exists",
                    content = @Content
            )
    })
    @SecurityRequirement(name = "")  // Publicly accessible endpoint
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            log.info("Signup request for email: {}", request.getEmail());
            AuthResponse response = authService.signup(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Signup error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Authenticate user and get JWT token",
            description = "Login with email and password. Returns JWT token valid for 24 hours. " +
                    "Use the token in the Authorization header for authenticated endpoints."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content
            )
    })
    @SecurityRequirement(name = "")  // Publicly accessible endpoint
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Login request for email: {}", request.getEmail());
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Health check endpoint",
            description = "Check if the authentication service is running and healthy"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service is healthy",
                    content = @Content
            )
    })
    @SecurityRequirement(name = "")  // Publicly accessible endpoint
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "quad-auth",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
