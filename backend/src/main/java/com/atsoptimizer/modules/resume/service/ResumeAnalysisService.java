package com.atsoptimizer.modules.resume.service;

import com.atsoptimizer.infra.ai.ResumeAnalysisAI;
import com.atsoptimizer.infra.ai.dto.ResumeAnalysisResult;
import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.ResumeAnalysis;
import com.atsoptimizer.modules.resume.exception.ResumeNotFoundException;
import com.atsoptimizer.modules.resume.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisService.class);

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisAI resumeAnalysisAi;

    public ResumeAnalysisService(
            ResumeRepository resumeRepository,
            ResumeAnalysisAI resumeAnalysisAi
    ) {
        this.resumeRepository = resumeRepository;
        this.resumeAnalysisAi = resumeAnalysisAi;
    }

    @Transactional
    public void processAnalysis(Long resumeId, String extractedText) {
        log.info("Starting AI analysis pipeline for resume id: {}", resumeId);

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResumeNotFoundException(resumeId));

        try {
            resume.markAsProcessing();
            resumeRepository.save(resume);

            ResumeAnalysisResult dto = resumeAnalysisAi.sendToAiAnalisysResume(extractedText);

            ResumeAnalysis analysis = new ResumeAnalysis(
                    resumeId,
                    dto.score(),
                    dto.inferredObjective(),
                    dto.overallProfile(),
                    dto.structureEvaluation(),
                    dto.atsCompatibilitySummary(),
                    dto.keywords(),
                    dto.missingKeywords(),
                    dto.inconsistencies(),
                    dto.strengths(),
                    dto.weaknesses(),
                    dto.improvementSuggestions(),
                    LocalDateTime.now()
            );

            resume.completeAnalysis(analysis);
            resumeRepository.save(resume);

            log.info("Successfully completed AI analysis for resume [{}]. ATS Score: {}%", resumeId, dto.score());
        } catch (Exception e) {
            log.error("Error during AI analysis for resume [{}]: {}", resumeId, e.getMessage(), e);
            throw e;
        }
    }
}
