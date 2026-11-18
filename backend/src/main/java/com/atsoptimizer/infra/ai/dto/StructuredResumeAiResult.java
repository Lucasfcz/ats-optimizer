package com.atsoptimizer.infra.ai.dto;

import java.util.List;

public record StructuredResumeAiResult(
        String suggestedFileName,
        String candidateName,
        String location,
        String phone,
        String email,
        String linkedin,
        List<String> otherLinks,
        String targetRoleTitle,
        String summary,
        List<SkillCategoryDto> technicalSkills,
        List<TemplateExperienceEntryDto> experiencesAndProjects,
        List<EducationDto> education,
        List<String> achievements,
        List<LanguageDto> languages,
        List<String> injectedGaps
) {
}
