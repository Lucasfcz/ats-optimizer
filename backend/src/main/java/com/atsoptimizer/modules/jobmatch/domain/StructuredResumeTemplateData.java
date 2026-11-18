package com.atsoptimizer.modules.jobmatch.domain;

import com.atsoptimizer.modules.resume.domain.EducationItem;
import com.atsoptimizer.modules.resume.domain.LanguageItem;
import com.atsoptimizer.modules.resume.domain.SkillCategory;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Complete structured resume content returned by the AI to be deterministically rendered
 * into a 1-page ATS-friendly PDF matching the official candidate template.
 */
public record StructuredResumeTemplateData(
        String suggestedFileName,
        String candidateName,
        String location,
        String phone,
        String email,
        String linkedin,
        List<String> otherLinks,
        String targetRoleTitle,
        String summary,
        List<SkillCategory> technicalSkills,
        List<TemplateExperienceEntry> experiencesAndProjects,
        List<EducationItem> education,
        List<String> achievements,
        List<LanguageItem> languages,
        List<String> injectedGaps
) implements Serializable {

    public StructuredResumeTemplateData {
        otherLinks = safeCopy(otherLinks);
        technicalSkills = safeCopy(technicalSkills);
        experiencesAndProjects = safeCopy(experiencesAndProjects);
        education = safeCopy(education);
        achievements = safeCopy(achievements);
        languages = safeCopy(languages);
        injectedGaps = safeCopy(injectedGaps);
    }

    public StructuredResumeTemplateData withExperiencesAndProjects(List<TemplateExperienceEntry> updatedExperiences) {
        return new StructuredResumeTemplateData(
                suggestedFileName,
                candidateName,
                location,
                phone,
                email,
                linkedin,
                otherLinks,
                targetRoleTitle,
                summary,
                technicalSkills,
                updatedExperiences,
                education,
                achievements,
                languages,
                injectedGaps
        );
    }

    private static <T> List<T> safeCopy(List<T> list) {
        return list != null ? List.copyOf(list) : Collections.emptyList();
    }
}
