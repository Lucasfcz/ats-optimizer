package com.atsoptimizer.modules.resume.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ResumeAnalysis implements Serializable {

    private Long resumeId;
    private Integer score;
    private String candidateName;
    private String headline;
    private ContactInfo contactInfo;
    private String about;
    private List<ExperienceItem> experiences;
    private List<EducationItem> education;
    private List<CertificationItem> certifications;
    private List<LanguageItem> languages;
    private List<SkillCategory> skillCategories;
    private List<String> achievements;
    private String inferredObjective;
    private String overallProfile;
    private String structureEvaluation;
    private String atsCompatibilitySummary;
    private List<String> keywords;
    private List<String> missingKeywords;
    private List<String> inconsistencies;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> improvementSuggestions;
    private LocalDateTime analyzedAt;

    protected ResumeAnalysis() {
    }

    public ResumeAnalysis(
            Long resumeId,
            Integer score,
            String candidateName,
            String headline,
            ContactInfo contactInfo,
            String about,
            List<ExperienceItem> experiences,
            List<EducationItem> education,
            List<CertificationItem> certifications,
            List<LanguageItem> languages,
            List<SkillCategory> skillCategories,
            List<String> achievements,
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
            LocalDateTime analyzedAt
    ) {
        this.resumeId = resumeId;
        this.score = score;
        this.candidateName = candidateName;
        this.headline = headline;
        this.contactInfo = contactInfo;
        this.about = about;
        this.experiences = safeCopy(experiences);
        this.education = safeCopy(education);
        this.certifications = safeCopy(certifications);
        this.languages = safeCopy(languages);
        this.skillCategories = safeCopy(skillCategories);
        this.achievements = safeCopy(achievements);
        this.inferredObjective = inferredObjective;
        this.overallProfile = overallProfile;
        this.structureEvaluation = structureEvaluation;
        this.atsCompatibilitySummary = atsCompatibilitySummary;
        this.keywords = safeCopy(keywords);
        this.missingKeywords = safeCopy(missingKeywords);
        this.inconsistencies = safeCopy(inconsistencies);
        this.strengths = safeCopy(strengths);
        this.weaknesses = safeCopy(weaknesses);
        this.improvementSuggestions = safeCopy(improvementSuggestions);
        this.analyzedAt = analyzedAt != null ? analyzedAt : LocalDateTime.now();
    }

    private static <T> List<T> safeCopy(List<T> list) {
        return list != null ? List.copyOf(list) : Collections.emptyList();
    }

    public Long getResumeId() {
        return resumeId;
    }

    public Integer getScore() {
        return score;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getHeadline() {
        return headline;
    }

    public ContactInfo getContactInfo() {
        return contactInfo;
    }

    public String getAbout() {
        return about;
    }

    public List<ExperienceItem> getExperiences() {
        return experiences;
    }

    public List<EducationItem> getEducation() {
        return education;
    }

    public List<CertificationItem> getCertifications() {
        return certifications;
    }

    public List<LanguageItem> getLanguages() {
        return languages;
    }

    public List<SkillCategory> getSkillCategories() {
        return skillCategories;
    }

    public List<String> getAchievements() {
        return achievements;
    }

    public String getInferredObjective() {
        return inferredObjective;
    }

    public String getOverallProfile() {
        return overallProfile;
    }

    public String getStructureEvaluation() {
        return structureEvaluation;
    }

    public String getAtsCompatibilitySummary() {
        return atsCompatibilitySummary;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public List<String> getInconsistencies() {
        return inconsistencies;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public List<String> getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }
}
