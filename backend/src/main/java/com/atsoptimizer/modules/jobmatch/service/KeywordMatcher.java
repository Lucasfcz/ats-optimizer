package com.atsoptimizer.modules.jobmatch.service;

import com.atsoptimizer.modules.jobmatch.domain.JobRequirements;
import com.atsoptimizer.modules.jobmatch.domain.JobSkillRequirement;
import com.atsoptimizer.modules.jobmatch.domain.KeywordMatchReport;
import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.ResumeAnalysis;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Deterministic keyword matching engine that evaluates extracted job requirements
 * (including synonyms/aliases) against the candidate's raw resume text and structured analysis.
 */
@Component
public class KeywordMatcher {

    public KeywordMatchReport evaluate(Resume resume, JobRequirements requirements) {
        String corpus = buildNormalizedCorpus(resume);

        List<String> matchedRequired = new ArrayList<>();
        List<String> missingRequired = new ArrayList<>();
        evaluateSkillList(requirements.requiredHardSkills(), corpus, matchedRequired, missingRequired);

        List<String> matchedPreferred = new ArrayList<>();
        List<String> missingPreferred = new ArrayList<>();
        evaluateSkillList(requirements.preferredHardSkills(), corpus, matchedPreferred, missingPreferred);

        int requiredPercentage = calculatePercentage(matchedRequired.size(), requirements.requiredHardSkills().size());
        int preferredPercentage = calculatePercentage(matchedPreferred.size(), requirements.preferredHardSkills().size());

        return new KeywordMatchReport(
                requiredPercentage,
                preferredPercentage,
                matchedRequired,
                missingRequired,
                matchedPreferred,
                missingPreferred
        );
    }

    private void evaluateSkillList(
            List<JobSkillRequirement> skills,
            String normalizedCorpus,
            List<String> matchedOut,
            List<String> missingOut
    ) {
        for (JobSkillRequirement skill : skills) {
            if (matchesAnyTerm(normalizedCorpus, skill)) {
                matchedOut.add(skill.canonicalName());
            } else {
                missingOut.add(skill.canonicalName());
            }
        }
    }

    private boolean matchesAnyTerm(String normalizedCorpus, JobSkillRequirement skill) {
        if (containsWholeTerm(normalizedCorpus, skill.canonicalName())) {
            return true;
        }
        for (String alias : skill.aliases()) {
            if (containsWholeTerm(normalizedCorpus, alias)) {
                return true;
            }
        }
        return false;
    }

    boolean containsWholeTerm(String normalizedCorpus, String rawTerm) {
        String term = normalizeText(rawTerm);
        if (term.isEmpty()) {
            return false;
        }
        String leftBoundary = term.startsWith(".") ? "(?<![\\p{Alnum}_+#])" : "(?<![\\p{Alnum}_+#.])";
        String rightBoundary = "(?![\\p{Alnum}_+#]|\\.[\\p{Alnum}])";
        Pattern pattern = Pattern.compile(leftBoundary + Pattern.quote(term) + rightBoundary);
        return pattern.matcher(normalizedCorpus).find();
    }

    private String buildNormalizedCorpus(Resume resume) {
        StringBuilder sb = new StringBuilder();
        if (resume.getExtractedText() != null) {
            sb.append(resume.getExtractedText()).append(' ');
        }
        ResumeAnalysis analysis = resume.getAnalysis();
        if (analysis != null) {
            appendIfNotNull(sb, analysis.getHeadline());
            appendIfNotNull(sb, analysis.getAbout());
            appendList(sb, analysis.getKeywords());
            appendList(sb, analysis.getAchievements());
            if (analysis.getSkillCategories() != null) {
                analysis.getSkillCategories().forEach(cat -> {
                    appendIfNotNull(sb, cat.categoryName());
                    appendList(sb, cat.skills());
                });
            }
            if (analysis.getExperiences() != null) {
                analysis.getExperiences().forEach(exp -> {
                    appendIfNotNull(sb, exp.title());
                    appendIfNotNull(sb, exp.organization());
                    appendIfNotNull(sb, exp.description());
                    appendList(sb, exp.highlights());
                });
            }
            if (analysis.getEducation() != null) {
                analysis.getEducation().forEach(edu -> {
                    appendIfNotNull(sb, edu.degree());
                    appendIfNotNull(sb, edu.fieldOfStudy());
                });
            }
            if (analysis.getCertifications() != null) {
                analysis.getCertifications().forEach(cert -> appendIfNotNull(sb, cert.name()));
            }
        }
        return normalizeText(sb.toString());
    }

    private void appendIfNotNull(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(' ');
        }
    }

    private void appendList(StringBuilder sb, List<String> values) {
        if (values != null) {
            values.forEach(v -> appendIfNotNull(sb, v));
        }
    }

    String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int calculatePercentage(int matched, int total) {
        if (total == 0) {
            return 100;
        }
        return (int) Math.round((matched * 100.0) / total);
    }
}
