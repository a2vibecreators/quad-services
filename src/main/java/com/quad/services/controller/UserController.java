package com.quad.services.controller;

import com.quad.services.entity.User;
import com.quad.services.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")  // TODO: Configure properly for production
public class UserController {

    private final UserService userService;

    /**
     * GET /users/email/{email}
     * Find user by email address (used by NextAuth OAuth callback for account linking)
     *
     * @param email User's email address (URL encoded)
     * @return User object if found, 404 if not found
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
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

    /**
     * GET /users/email/{email}/exists
     * Check if user exists without returning full user data
     *
     * @param email User's email address (URL encoded)
     * @return { "exists": true/false }
     */
    @GetMapping("/email/{email}/exists")
    public ResponseEntity<?> checkUserExists(@PathVariable String email) {
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
