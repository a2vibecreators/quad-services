package com.quad.services.service;

import com.quad.services.dto.AgentRulesResponse;
import com.quad.services.entity.IndustryDefault;
import com.quad.services.entity.Organization;
import com.quad.services.repository.IndustryDefaultRepository;
import com.quad.services.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for fetching and merging agent rules.
 *
 * Flow:
 * 1. Get org's industry from Organization table
 * 2. Fetch industry defaults from IndustryDefault table
 * 3. (Future) Fetch org customizations and merge
 * 4. Return merged rules grouped by DO/DONT
 */
@Service
public class AgentRulesService {

    @Autowired
    private IndustryDefaultRepository industryDefaultRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Get merged rules for an organization and activity type.
     * CACHED: 5 minutes TTL - rules don't change often.
     *
     * @param orgId        Organization UUID
     * @param activityType Activity type (e.g., "add_api_endpoint")
     * @return Merged rules response
     */
    @Cacheable(value = "agentRules", key = "#orgId + '-' + #activityType")
    public AgentRulesResponse getMergedRules(UUID orgId, String activityType) {
        // 1. Get organization's industry
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + orgId));

        String industry = org.getIndustry();
        if (industry == null || industry.isEmpty()) {
            industry = "general";  // Default fallback
        }

        // 2. Fetch industry defaults
        List<IndustryDefault> defaults = industryDefaultRepository
                .findByIndustryAndActivityType(industry, activityType);

        // 3. Group rules by type (DO / DONT)
        Map<String, List<String>> rules = new HashMap<>();
        rules.put("DO", new ArrayList<>());
        rules.put("DONT", new ArrayList<>());

        for (IndustryDefault rule : defaults) {
            String ruleType = rule.getRuleType();
            if ("DO".equals(ruleType) || "DONT".equals(ruleType)) {
                rules.get(ruleType).add(rule.getRuleText());
            }
        }

        // 4. Return response
        return new AgentRulesResponse(activityType, industry, rules);
    }

    /**
     * Get rules by industry directly (without org lookup).
     * CACHED: 5 minutes TTL.
     * Useful for testing or when industry is known.
     */
    @Cacheable(value = "agentRules", key = "'industry-' + #industry + '-' + #activityType")
    public AgentRulesResponse getRulesByIndustry(String industry, String activityType) {
        List<IndustryDefault> defaults = industryDefaultRepository
                .findByIndustryAndActivityType(industry, activityType);

        Map<String, List<String>> rules = new HashMap<>();
        rules.put("DO", defaults.stream()
                .filter(r -> "DO".equals(r.getRuleType()))
                .map(IndustryDefault::getRuleText)
                .collect(Collectors.toList()));
        rules.put("DONT", defaults.stream()
                .filter(r -> "DONT".equals(r.getRuleType()))
                .map(IndustryDefault::getRuleText)
                .collect(Collectors.toList()));

        return new AgentRulesResponse(activityType, industry, rules);
    }

    /**
     * Clear cache when rules are updated.
     * Call this when admin updates industry defaults or org customizations.
     */
    @CacheEvict(value = "agentRules", allEntries = true)
    public void clearRulesCache() {
        // Cache will be cleared by Spring
    }
}
