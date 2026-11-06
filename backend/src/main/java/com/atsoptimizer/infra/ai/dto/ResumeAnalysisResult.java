package com.atsoptimizer.infra.ai.dto;

import java.util.List;

public record ResumeAnalysisResult(
        String candidateName,
        String headline,
        ContactInfoDto contactInfo,
        String about,
        List<ExperienceDto> experiences,
        List<EducationDto> education,
        List<CertificationDto> certifications,
        List<LanguageDto> languages,
        String inferredObjective,
        String overallProfile,
        String structureEvaluation,
        String atsCompatibilitySummary,
        List<String> keywords,
        List<String> missingKeywords,
        List<String> inconsistencies,
        List<String> strengths,
        List<String> weaknesses,
        List<String> improvementSuggestions,
        Integer score
) {
}
