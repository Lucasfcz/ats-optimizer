package com.atsoptimizer.infra.ai.dto;

import java.util.List;

public record JobFitAuditAiResult(
        Integer semanticFitScore,
        Integer structureImpactScore,
        String executiveSummary,
        List<String> matchReasons,
        List<String> criticalGaps,
        List<String> improvementRecommendations
) {
}
