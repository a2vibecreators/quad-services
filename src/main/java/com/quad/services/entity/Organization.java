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
@Table(name = "quad_organizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String slug;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "billing_email")
    private String billingEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    private String website;

    private String industry;

    @Column(name = "team_size")
    private String teamSize;

    private String timezone;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "ai_tier")
    private String aiTier;

    @Column(name = "sandbox_strategy")
    private String sandboxStrategy;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
