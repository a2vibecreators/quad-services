package com.quad.services.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String password;  // Optional for passwordless flow

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Company name is required")
    private String companyName;

    private String orgType;  // "startup", "business", "enterprise"
    private Boolean isOAuth;
    private String oauthProvider;
    private Boolean isEmailVerified;
}
