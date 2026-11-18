package com.atsoptimizer.modules.jobmatch.service;

import com.atsoptimizer.infra.ai.IdealResumeFormatterAI;
import com.atsoptimizer.infra.ai.JobDescriptionParserAI;
import com.atsoptimizer.infra.ai.JobFitAuditAI;
import com.atsoptimizer.infra.ai.TruthfulResumeFormatterAI;
import com.atsoptimizer.infra.ai.dto.EducationDto;
import com.atsoptimizer.infra.ai.dto.JobFitAuditAiResult;
import com.atsoptimizer.infra.ai.dto.JobRequirementsAiResult;
import com.atsoptimizer.infra.ai.dto.JobSkillRequirementDto;
import com.atsoptimizer.infra.ai.dto.LanguageDto;
import com.atsoptimizer.infra.ai.dto.SkillCategoryDto;
import com.atsoptimizer.infra.ai.dto.StructuredResumeAiResult;
import com.atsoptimizer.infra.ai.dto.TemplateExperienceEntryDto;
import com.atsoptimizer.infra.message.JobMatchEventPublisher;
import com.atsoptimizer.infra.message.dto.JobMatchMessage;
import com.atsoptimizer.modules.jobmatch.domain.JobFitAudit;
import com.atsoptimizer.modules.jobmatch.domain.JobMatch;
import com.atsoptimizer.modules.jobmatch.domain.JobRequirements;
import com.atsoptimizer.modules.jobmatch.domain.JobSkillRequirement;
import com.atsoptimizer.modules.jobmatch.domain.KeywordMatchReport;
import com.atsoptimizer.modules.jobmatch.domain.OptimizationMode;
import com.atsoptimizer.modules.jobmatch.domain.StructuredResumeTemplateData;
import com.atsoptimizer.modules.jobmatch.domain.TemplateExperienceEntry;
import com.atsoptimizer.modules.jobmatch.exception.JobMatchNotFoundException;
import com.atsoptimizer.modules.jobmatch.repository.JobMatchRepository;
import com.atsoptimizer.modules.resume.domain.EducationItem;
import com.atsoptimizer.modules.resume.domain.LanguageItem;
import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.SkillCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Orchestrates asynchronous RabbitMQ processing stages for job matching:
 * Stage 1 (Parse + KeywordMatcher) -> Stage 2 (Fit Audit) -> Stage 3 (On-demand Resume Text + 1-page verification).
 */
@Service
public class JobMatchProcessingService {

    private static final Logger log = LoggerFactory.getLogger(JobMatchProcessingService.class);

    private final JobMatchRepository jobMatchRepository;
    private final JobDescriptionParserAI jobDescriptionParserAI;
    private final JobFitAuditAI jobFitAuditAI;
    private final TruthfulResumeFormatterAI truthfulResumeFormatterAI;
    private final IdealResumeFormatterAI idealResumeFormatterAI;
    private final KeywordMatcher keywordMatcher;
    private final ResumePdfGeneratorService resumePdfGeneratorService;
    private final JobMatchEventPublisher jobMatchEventPublisher;

    public JobMatchProcessingService(
            JobMatchRepository jobMatchRepository,
            JobDescriptionParserAI jobDescriptionParserAI,
            JobFitAuditAI jobFitAuditAI,
            TruthfulResumeFormatterAI truthfulResumeFormatterAI,
            IdealResumeFormatterAI idealResumeFormatterAI,
            KeywordMatcher keywordMatcher,
            ResumePdfGeneratorService resumePdfGeneratorService,
            JobMatchEventPublisher jobMatchEventPublisher
    ) {
        this.jobMatchRepository = jobMatchRepository;
        this.jobDescriptionParserAI = jobDescriptionParserAI;
        this.jobFitAuditAI = jobFitAuditAI;
        this.truthfulResumeFormatterAI = truthfulResumeFormatterAI;
        this.idealResumeFormatterAI = idealResumeFormatterAI;
        this.keywordMatcher = keywordMatcher;
        this.resumePdfGeneratorService = resumePdfGeneratorService;
        this.jobMatchEventPublisher = jobMatchEventPublisher;
    }

    @Transactional
    public void processJobParsing(Long jobMatchId) {
        log.info("Stage 1 [PARSE]: Processing job parsing for jobMatch [{}]", jobMatchId);
        JobMatch jobMatch = loadJobMatchOrThrow(jobMatchId);
        jobMatch.markAsParsingJob();
        jobMatchRepository.save(jobMatch);

        JobRequirementsAiResult aiResult = jobDescriptionParserAI.extractRequirements(jobMatch.getJobDescription());
        JobRequirements requirements = mapJobRequirements(aiResult);
        KeywordMatchReport keywordReport = keywordMatcher.evaluate(jobMatch.getResume(), requirements);

        jobMatch.completeJobParsing(requirements, keywordReport);
        jobMatchRepository.save(jobMatch);

        log.info("Stage 1 [PARSE] completed for jobMatch [{}]. Required match: {}%, Preferred match: {}%",
                jobMatchId, keywordReport.requiredSkillsMatchPercentage(), keywordReport.preferredSkillsMatchPercentage());

        jobMatchEventPublisher.publishForFitAudit(new JobMatchMessage(jobMatchId, jobMatch.getResume().getId()));
    }

    @Transactional
    public void processFitAudit(Long jobMatchId) {
        log.info("Stage 2 [AUDIT]: Running semantic ATS fit audit for jobMatch [{}]", jobMatchId);
        JobMatch jobMatch = loadJobMatchOrThrow(jobMatchId);
        Resume resume = jobMatch.getResume();

        JobFitAuditAiResult aiAudit = jobFitAuditAI.evaluateFit(
                resume.getAnalysis(),
                resume.getExtractedText(),
                jobMatch.getJobRequirements(),
                jobMatch.getKeywordMatchReport(),
                jobMatch.getJobDescription()
        );

        int compositeScore = calculateCompositeScore(jobMatch.getKeywordMatchReport(), aiAudit);
        JobFitAudit audit = new JobFitAudit(
                compositeScore,
                clampScore(aiAudit.semanticFitScore()),
                clampScore(aiAudit.structureImpactScore()),
                aiAudit.executiveSummary(),
                aiAudit.matchReasons(),
                aiAudit.criticalGaps(),
                aiAudit.improvementRecommendations()
        );

        jobMatch.completeFitAudit(audit);
        jobMatchRepository.save(jobMatch);

        log.info("Stage 2 [AUDIT] completed for jobMatch [{}]. Composite ATS Match Score: {}%",
                jobMatchId, compositeScore);
    }

    @Transactional
    public void processResumeGeneration(Long jobMatchId, OptimizationMode mode) {
        log.info("Stage 3 [GENERATE]: Generating [{}] resume data for jobMatch [{}]", mode, jobMatchId);
        JobMatch jobMatch = loadJobMatchOrThrow(jobMatchId);
        jobMatch.markAsGeneratingResume();
        jobMatchRepository.save(jobMatch);

        Resume resume = jobMatch.getResume();
        StructuredResumeAiResult aiResult = invokeFormatterAi(mode, resume, jobMatch);
        StructuredResumeTemplateData structuredData = mapTemplateData(aiResult, mode);

        if (!resumePdfGeneratorService.fitsInSinglePage(structuredData)) {
            log.info("Generated [{}] resume for jobMatch [{}] exceeded 1 page; running AI condense pass", mode, jobMatchId);
            StructuredResumeAiResult condensed = invokeCondenseAi(mode, structuredData);
            structuredData = mapTemplateData(condensed, mode);
        }

        structuredData = resumePdfGeneratorService.pruneToSinglePageIfNeeded(structuredData);

        jobMatch.completeResumeGeneration(mode, structuredData);
        jobMatchRepository.save(jobMatch);

        log.info("Stage 3 [GENERATE] completed for jobMatch [{}] mode [{}]. File: {}",
                jobMatchId, mode, structuredData.suggestedFileName());
    }

    private int calculateCompositeScore(KeywordMatchReport report, JobFitAuditAiResult aiAudit) {
        int requiredPct = report.requiredSkillsMatchPercentage();
        int preferredPct = report.preferredSkillsMatchPercentage();
        int semanticPct = clampScore(aiAudit.semanticFitScore());
        int structurePct = clampScore(aiAudit.structureImpactScore());

        double weighted = (requiredPct * 0.50)
                + (preferredPct * 0.15)
                + (semanticPct * 0.25)
                + (structurePct * 0.10);

        return clampScore((int) Math.round(weighted));
    }

    private StructuredResumeAiResult invokeFormatterAi(OptimizationMode mode, Resume resume, JobMatch jobMatch) {
        if (mode == OptimizationMode.TRUTHFUL) {
            return truthfulResumeFormatterAI.generate(
                    resume.getAnalysis(),
                    resume.getExtractedText(),
                    jobMatch.getJobRequirements(),
                    jobMatch.getKeywordMatchReport(),
                    jobMatch.getFitAudit()
            );
        }
        return idealResumeFormatterAI.generate(
                resume.getAnalysis(),
                resume.getExtractedText(),
                jobMatch.getJobRequirements(),
                jobMatch.getKeywordMatchReport(),
                jobMatch.getFitAudit()
        );
    }

    private StructuredResumeAiResult invokeCondenseAi(OptimizationMode mode, StructuredResumeTemplateData oversized) {
        if (mode == OptimizationMode.TRUTHFUL) {
            return truthfulResumeFormatterAI.condenseToSinglePage(oversized);
        }
        return idealResumeFormatterAI.condenseToSinglePage(oversized);
    }

    private JobMatch loadJobMatchOrThrow(Long jobMatchId) {
        return jobMatchRepository.findWithResumeById(jobMatchId)
                .orElseThrow(() -> new JobMatchNotFoundException(jobMatchId));
    }

    private int clampScore(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value));
    }

    private JobRequirements mapJobRequirements(JobRequirementsAiResult dto) {
        return new JobRequirements(
                dto.jobTitle(),
                dto.seniorityLevel(),
                mapList(dto.requiredHardSkills(), this::mapSkillRequirement),
                mapList(dto.preferredHardSkills(), this::mapSkillRequirement),
                dto.softSkills(),
                dto.responsibilities(),
                dto.domainKeywords()
        );
    }

    private JobSkillRequirement mapSkillRequirement(JobSkillRequirementDto dto) {
        return new JobSkillRequirement(dto.canonicalName(), dto.aliases());
    }

    private StructuredResumeTemplateData mapTemplateData(StructuredResumeAiResult dto, OptimizationMode mode) {
        String safeFileName = sanitizePdfFileName(dto.suggestedFileName(), mode);
        List<String> gaps = mode == OptimizationMode.TRUTHFUL
                ? Collections.emptyList()
                : dto.injectedGaps();

        return new StructuredResumeTemplateData(
                safeFileName,
                dto.candidateName(),
                dto.location(),
                dto.phone(),
                dto.email(),
                dto.linkedin(),
                dto.otherLinks(),
                dto.targetRoleTitle(),
                dto.summary(),
                mapList(dto.technicalSkills(), this::mapSkillCategory),
                mapList(dto.experiencesAndProjects(), this::mapExperienceEntry),
                mapList(dto.education(), this::mapEducation),
                dto.achievements(),
                mapList(dto.languages(), this::mapLanguage),
                gaps
        );
    }

    private String sanitizePdfFileName(String rawName, OptimizationMode mode) {
        if (rawName == null || rawName.isBlank()) {
            return "curriculo_otimizado_" + mode.name().toLowerCase() + ".pdf";
        }
        String sanitized = rawName.trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");
        if (!sanitized.toLowerCase().endsWith(".pdf")) {
            sanitized = sanitized + ".pdf";
        }
        return sanitized;
    }

    private SkillCategory mapSkillCategory(SkillCategoryDto dto) {
        return new SkillCategory(dto.categoryName(), dto.skills());
    }

    private TemplateExperienceEntry mapExperienceEntry(TemplateExperienceEntryDto dto) {
        return new TemplateExperienceEntry(
                dto.title(),
                dto.period(),
                dto.subtitleLabel(),
                Boolean.TRUE.equals(dto.isStack()),
                dto.bullets()
        );
    }

    private EducationItem mapEducation(EducationDto dto) {
        return new EducationItem(dto.institution(), dto.degree(), dto.fieldOfStudy(), dto.period(), dto.status());
    }

    private LanguageItem mapLanguage(LanguageDto dto) {
        return new LanguageItem(dto.language(), dto.proficiency());
    }

    private <D, T> List<T> mapList(List<D> source, Function<D, T> mapper) {
        if (source == null) {
            return Collections.emptyList();
        }
        return source.stream().map(mapper).toList();
    }
}
