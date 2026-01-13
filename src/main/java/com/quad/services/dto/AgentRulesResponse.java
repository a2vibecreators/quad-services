package com.quad.services.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for agent rules API.
 *
 * Returns merged rules (industry defaults + org customizations)
 * that Story Agent uses to guide Claude AI code generation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentRulesResponse {

    private String activityType;  // "add_api_endpoint"
    private String industry;      // "investment_banking"

    // Rules grouped by type: { "DO": [...], "DONT": [...] }
    private Map<String, List<String>> rules;
}
