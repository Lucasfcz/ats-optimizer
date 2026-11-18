package com.atsoptimizer.modules.jobmatch.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic keyword matching report produced in Java comparing extracted job skills
 * against the candidate's resume text and structured profile.
 */
public record KeywordMatchReport(
        int requiredSkillsMatchPercentage,
        int preferredSkillsMatchPercentage,
        List<String> matchedRequiredSkills,
        List<String> missingRequiredSkills,
        List<String> matchedPreferredSkills,
        List<String> missingPreferredSkills
) implements Serializable {

    public KeywordMatchReport {
        matchedRequiredSkills = matchedRequiredSkills != null ? List.copyOf(matchedRequiredSkills) : Collections.emptyList();
        missingRequiredSkills = missingRequiredSkills != null ? List.copyOf(missingRequiredSkills) : Collections.emptyList();
        matchedPreferredSkills = matchedPreferredSkills != null ? List.copyOf(matchedPreferredSkills) : Collections.emptyList();
        missingPreferredSkills = missingPreferredSkills != null ? List.copyOf(missingPreferredSkills) : Collections.emptyList();
    }
}
