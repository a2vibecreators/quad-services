package com.quad.services.controller;

import com.quad.services.entity.User;
import com.quad.services.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * User Management Controller
 *
 * Handles user lookup and profile operations. Currently used primarily for OAuth account linking.
 * All endpoints in this controller are publicly accessible for OAuth integration.
 *
 * @author QUAD Platform
 * @since 1.0.0
 */
@RestController
@RequestMapping("${api.version.prefix:/v1}/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")  // TODO: Configure properly for production
@Tag(name = "User Management", description = "User profile and lookup endpoints")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Find user by email address",
            description = "Lookup user account by email. Used by OAuth providers for account linking. " +
                    "Returns full user profile if found."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User found",
                    content = @Content(schema = @Schema(implementation = User.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content
            )
    })
    @SecurityRequirement(name = "")  // Publicly accessible endpoint
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(
            @Parameter(description = "User's email address", required = true, example = "user@example.com")
            @PathVariable String email
    ) {
        try {
            log.info("GET /users/email/{} - Looking up user", email);

            Optional<User> user = userService.findByEmail(email);

            if (user.isEmpty()) {
                log.info("User not found: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }

            log.info("User found: {} (id: {})", email, user.get().getId());
            return ResponseEntity.ok(user.get());

        } catch (Exception e) {
            log.error("Error looking up user by email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get user by email"));
        }
    }

    @Operation(
            summary = "Check if user exists by email",
            description = "Check if a user account exists without returning full profile data. " +
                    "Useful for validation during signup or account linking."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Check completed successfully",
                    content = @Content
            )
    })
    @SecurityRequirement(name = "")  // Publicly accessible endpoint
    @GetMapping("/email/{email}/exists")
    public ResponseEntity<?> checkUserExists(
            @Parameter(description = "User's email address", required = true, example = "user@example.com")
            @PathVariable String email
    ) {
        try {
            log.info("GET /users/email/{}/exists - Checking if user exists", email);
            boolean exists = userService.existsByEmail(email);
            log.info("User exists check for {}: {}", email, exists);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            log.error("Error checking user existence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check user existence"));
        }
    }
}
