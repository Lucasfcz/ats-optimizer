package com.atsoptimizer.modules.jobmatch.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Combined ATS audit combining deterministic keyword scores with semantic evaluation
 * from Axis 2 AI.
 */
public record JobFitAudit(
        int finalMatchScore,
        int semanticFitScore,
        int structureImpactScore,
        String executiveSummary,
        List<String> matchReasons,
        List<String> criticalGaps,
        List<String> improvementRecommendations
) implements Serializable {

    public JobFitAudit {
        matchReasons = matchReasons != null ? List.copyOf(matchReasons) : Collections.emptyList();
        criticalGaps = criticalGaps != null ? List.copyOf(criticalGaps) : Collections.emptyList();
        improvementRecommendations = improvementRecommendations != null
                ? List.copyOf(improvementRecommendations)
                : Collections.emptyList();
    }
}
