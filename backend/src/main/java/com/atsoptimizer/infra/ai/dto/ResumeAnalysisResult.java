package com.atsoptimizer.infra.ai.dto;

import java.util.List;

public record ResumeAnalysisResult(
        List<String> keywords,
        List<String> missingKeywords,
        String inferredObjective,
        String overallProfile,
        String structureEvaluation,
        List<String> inconsistencies,
        List<String> strengths,
        List<String> weaknesses,
        List<String> improvementSuggestions,
        Integer score,
        String atsCompatibilitySummary
) {
}
