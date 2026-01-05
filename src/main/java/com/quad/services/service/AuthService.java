package com.quad.services.service;

import com.quad.services.dto.AuthResponse;
import com.quad.services.dto.LoginRequest;
import com.quad.services.dto.SignupRequest;
import com.quad.services.entity.Organization;
import com.quad.services.entity.User;
import com.quad.services.repository.OrganizationRepository;
import com.quad.services.repository.UserRepository;
import com.quad.services.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // Validate email not already registered
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new RuntimeException("Email already registered");
        }

        // Create organization
        Organization organization = new Organization();
        organization.setName(request.getCompanyName());
        organization.setSlug(generateSlug(request.getCompanyName()));
        organization.setIsActive(true);
        organization = organizationRepository.save(organization);

        log.info("Created organization: {} with ID: {}", organization.getName(), organization.getId());

        // Determine if passwordless
        boolean isPasswordless = "startup".equals(request.getOrgType()) ||
                                  "business".equals(request.getOrgType());

        String finalPassword = isPasswordless ? generateRandomPassword() : request.getPassword();

        if (!isPasswordless && (request.getPassword() == null || request.getPassword().isBlank())) {
            throw new RuntimeException("Password is required for this signup type");
        }

        // Create user
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(finalPassword));
        user.setFullName(request.getFullName());
        user.setName(request.getFullName());
        user.setRole("OWNER");
        user.setOrgId(organization.getId());
        user.setCompanyId(organization.getId());  // Backward compatibility
        user.setIsActive(true);
        user.setIsAdmin(true);

        // Set email verification status
        if (Boolean.TRUE.equals(request.getIsOAuth()) || Boolean.TRUE.equals(request.getIsEmailVerified())) {
            user.setEmailVerified(true);
        } else if (isPasswordless) {
            user.setEmailVerified(false);  // Will need OTP verification
        } else {
            user.setEmailVerified(true);  // Password signup is auto-verified
        }

        user = userRepository.save(user);

        log.info("Created user: {} with ID: {} for org: {}", user.getEmail(), user.getId(), organization.getId());

        // For OAuth or email-verified signups, return token immediately
        if (Boolean.TRUE.equals(request.getIsEmailVerified()) || !isPasswordless) {
            String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole(), organization.getId());

            return AuthResponse.builder()
                    .success(true)
                    .token(token)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .orgId(organization.getId())
                    .orgName(organization.getName())
                    .requiresVerification(false)
                    .build();
        }

        // For passwordless (non-OAuth), requires OTP verification
        // TODO: Generate and send OTP
        return AuthResponse.builder()
                .success(true)
                .userId(user.getId())
                .email(user.getEmail())
                .requiresVerification(true)
                .message("Verification code sent to " + user.getEmail())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Account is inactive");
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole(), user.getOrgId());

        // Get organization name
        String orgName = user.getOrgId() != null ?
                organizationRepository.findById(user.getOrgId())
                        .map(Organization::getName)
                        .orElse(null) : null;

        return AuthResponse.builder()
                .success(true)
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .orgId(user.getOrgId())
                .orgName(orgName)
                .requiresVerification(false)
                .build();
    }

    private String generateSlug(String name) {
        String slug = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();

        // Ensure uniqueness
        String finalSlug = slug;
        int counter = 1;
        while (organizationRepository.existsBySlug(finalSlug)) {
            finalSlug = slug + "-" + counter++;
        }

        return finalSlug;
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
