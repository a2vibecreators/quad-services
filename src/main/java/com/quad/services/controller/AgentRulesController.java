package com.quad.services.controller;

import com.quad.services.dto.AgentRulesResponse;
import com.quad.services.service.AgentRulesService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Agent Rules Controller
 *
 * Provides coding rules (DO's and DON'Ts) for AI code generation.
 * Used by Story Agent (VS Code Extension) to fetch rules before generating code.
 *
 * Flow:
 * 1. VS Code Extension detects activity type (e.g., "add_api_endpoint")
 * 2. Extension calls GET /api/agent-rules?orgId=xxx&activityType=add_api_endpoint
 * 3. This API returns industry defaults + org customizations
 * 4. Extension passes rules to Claude AI
 * 5. Claude generates compliant code
 *
 * @author QUAD Platform
 * @since 1.0.0
 */
@RestController
@RequestMapping("${api.version.prefix:/v1}/agent-rules")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Agent Rules", description = "Coding rules for AI code generation (Story Agent)")
public class AgentRulesController {

    private final AgentRulesService agentRulesService;

    @Operation(
            summary = "Get coding rules for an organization and activity",
            description = "Returns merged rules (industry defaults + org customizations) that guide AI code generation. " +
                    "Used by Story Agent (VS Code Extension) before generating code."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rules retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AgentRulesResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization not found",
                    content = @Content
            )
    })
    @SecurityRequirement(name = "")  // Publicly accessible for VS Code extension
    @GetMapping
    public ResponseEntity<?> getRules(
            @Parameter(description = "Organization UUID", required = true)
            @RequestParam("orgId") UUID orgId,

            @Parameter(description = "Activity type (e.g., add_api_endpoint, create_ui_screen)", required = true)
            @RequestParam("activityType") String activityType
    ) {
        try {
            log.info("Fetching rules for org: {}, activity: {}", orgId, activityType);
            AgentRulesResponse response = agentRulesService.getMergedRules(orgId, activityType);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error fetching rules: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Get rules by industry (without org lookup)",
            description = "Returns industry default rules directly. Useful for testing or when industry is known."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rules retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AgentRulesResponse.class))
            )
    })
    @SecurityRequirement(name = "")
    @GetMapping("/by-industry")
    public ResponseEntity<?> getRulesByIndustry(
            @Parameter(description = "Industry (e.g., investment_banking, healthcare, ecommerce)", required = true)
            @RequestParam("industry") String industry,

            @Parameter(description = "Activity type", required = true)
            @RequestParam("activityType") String activityType
    ) {
        log.info("Fetching rules for industry: {}, activity: {}", industry, activityType);
        AgentRulesResponse response = agentRulesService.getRulesByIndustry(industry, activityType);
        return ResponseEntity.ok(response);
    }
}
