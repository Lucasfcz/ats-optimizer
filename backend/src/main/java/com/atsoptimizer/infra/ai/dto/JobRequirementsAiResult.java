package com.atsoptimizer.infra.ai.dto;

import java.util.List;

public record JobRequirementsAiResult(
        String jobTitle,
        String seniorityLevel,
        List<JobSkillRequirementDto> requiredHardSkills,
        List<JobSkillRequirementDto> preferredHardSkills,
        List<String> softSkills,
        List<String> responsibilities,
        List<String> domainKeywords
) {
}
