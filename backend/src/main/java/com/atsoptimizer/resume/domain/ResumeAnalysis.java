package com.atsoptimizer.resume.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResumeAnalysis {

    private Long resumeId;
    private Integer score;
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
        // Construtor vazio protegido para frameworks de serialização (Jackson / Hibernate)
    }

    public ResumeAnalysis(
            Long resumeId,
            Integer score,
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
        this.inferredObjective = inferredObjective;
        this.overallProfile = overallProfile;
        this.structureEvaluation = structureEvaluation;
        this.atsCompatibilitySummary = atsCompatibilitySummary;
        this.keywords = keywords != null ? Collections.unmodifiableList(new ArrayList<>(keywords)) : Collections.emptyList();
        this.missingKeywords = missingKeywords != null ? Collections.unmodifiableList(new ArrayList<>(missingKeywords)) : Collections.emptyList();
        this.inconsistencies = inconsistencies != null ? Collections.unmodifiableList(new ArrayList<>(inconsistencies)) : Collections.emptyList();
        this.strengths = strengths != null ? Collections.unmodifiableList(new ArrayList<>(strengths)) : Collections.emptyList();
        this.weaknesses = weaknesses != null ? Collections.unmodifiableList(new ArrayList<>(weaknesses)) : Collections.emptyList();
        this.improvementSuggestions = improvementSuggestions != null ? Collections.unmodifiableList(new ArrayList<>(improvementSuggestions)) : Collections.emptyList();
        this.analyzedAt = analyzedAt != null ? analyzedAt : LocalDateTime.now();
    }

    public Long getResumeId() {
        return resumeId;
    }

    public Integer getScore() {
        return score;
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
