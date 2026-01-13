package com.quad.services.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Industry-standard coding rules that guide AI code generation.
 *
 * Example: Investment Banking industry has rules like:
 * - DO: Use Java Spring Boot
 * - DO: Add FINRA compliance logging
 * - DONT: Store PII in logs
 */
@Entity
@Table(name = "quad_industry_defaults")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndustryDefault {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String industry;  // "investment_banking", "healthcare", "ecommerce"

    @Column(name = "activity_type", nullable = false)
    private String activityType;  // "add_api_endpoint", "create_ui_screen"

    @Column(name = "rule_type", nullable = false)
    private String ruleType;  // "DO" or "DONT"

    @Column(name = "rule_text", nullable = false)
    private String ruleText;  // Human-readable rule

    private Integer priority = 100;  // Industry defaults = 100

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
