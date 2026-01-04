package com.quad.services.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quad_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    private String name;

    @Column(name = "full_name")
    private String fullName;

    private String role;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "org_id")
    private UUID orgId;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String department;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "github_username")
    private String githubUsername;

    @Column(name = "slack_user_id")
    private String slackUserId;

    private String timezone;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
