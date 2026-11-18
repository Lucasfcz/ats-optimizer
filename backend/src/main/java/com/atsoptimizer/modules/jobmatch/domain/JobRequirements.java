package com.atsoptimizer.modules.jobmatch.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Structured extraction of a job description produced by the Axis 1 AI parser.
 */
public record JobRequirements(
        String jobTitle,
        String seniorityLevel,
        List<JobSkillRequirement> requiredHardSkills,
        List<JobSkillRequirement> preferredHardSkills,
        List<String> softSkills,
        List<String> responsibilities,
        List<String> domainKeywords
) implements Serializable {

    public JobRequirements {
        requiredHardSkills = requiredHardSkills != null ? List.copyOf(requiredHardSkills) : Collections.emptyList();
        preferredHardSkills = preferredHardSkills != null ? List.copyOf(preferredHardSkills) : Collections.emptyList();
        softSkills = softSkills != null ? List.copyOf(softSkills) : Collections.emptyList();
        responsibilities = responsibilities != null ? List.copyOf(responsibilities) : Collections.emptyList();
        domainKeywords = domainKeywords != null ? List.copyOf(domainKeywords) : Collections.emptyList();
    }
}
