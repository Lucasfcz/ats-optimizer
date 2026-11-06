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
        this.experiences = experiences != null ? List.copyOf(experiences) : Collections.emptyList();
        this.education = education != null ? List.copyOf(education) : Collections.emptyList();
        this.certifications = certifications != null ? List.copyOf(certifications) : Collections.emptyList();
        this.languages = languages != null ? List.copyOf(languages) : Collections.emptyList();
        this.inferredObjective = inferredObjective;
        this.overallProfile = overallProfile;
        this.structureEvaluation = structureEvaluation;
        this.atsCompatibilitySummary = atsCompatibilitySummary;
        this.keywords = keywords != null ? List.copyOf(keywords) : Collections.emptyList();
        this.missingKeywords = missingKeywords != null ? List.copyOf(missingKeywords) : Collections.emptyList();
        this.inconsistencies = inconsistencies != null ? List.copyOf(inconsistencies) : Collections.emptyList();
        this.strengths = strengths != null ? List.copyOf(strengths) : Collections.emptyList();
        this.weaknesses = weaknesses != null ? List.copyOf(weaknesses) : Collections.emptyList();
        this.improvementSuggestions = improvementSuggestions != null ? List.copyOf(improvementSuggestions) : Collections.emptyList();
        this.analyzedAt = analyzedAt != null ? analyzedAt : LocalDateTime.now();
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
