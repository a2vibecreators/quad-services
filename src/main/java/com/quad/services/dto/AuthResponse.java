package com.quad.services.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UUID userId;
    private String email;
    private String fullName;
    private String role;
    private UUID orgId;
    private String orgName;
    private Boolean requiresVerification;
    private String message;
}
