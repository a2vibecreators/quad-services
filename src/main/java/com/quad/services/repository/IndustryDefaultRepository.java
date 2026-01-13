package com.quad.services.repository;

import com.quad.services.entity.IndustryDefault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IndustryDefaultRepository extends JpaRepository<IndustryDefault, UUID> {

    /**
     * Find all rules for a specific industry and activity type.
     * Used by Story Agent to fetch coding rules before code generation.
     */
    List<IndustryDefault> findByIndustryAndActivityType(String industry, String activityType);

    /**
     * Find all rules for an industry (all activity types).
     */
    List<IndustryDefault> findByIndustry(String industry);
}
