package com.atsoptimizer.resume;

import com.atsoptimizer.ai.AiClient;
import com.atsoptimizer.ai.ResumeAnalysisPromptBuilder;
import com.atsoptimizer.ai.dto.ResumeAnalysisResult;
import com.atsoptimizer.resume.domain.Resume;
import com.atsoptimizer.resume.domain.ResumeAnalysis;
import com.atsoptimizer.resume.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisService.class);

    private final ResumeRepository resumeRepository;
    private final AiClient aiClient;
    private final ResumeAnalysisPromptBuilder promptBuilder;

    public ResumeAnalysisService(
            ResumeRepository resumeRepository,
            AiClient aiClient,
            ResumeAnalysisPromptBuilder promptBuilder
    ) {
        this.resumeRepository = resumeRepository;
        this.aiClient = aiClient;
        this.promptBuilder = promptBuilder;
    }

    @Transactional
    public void processAnalysis(Long resumeId, String extractedText) {
        log.info("Starting AI analysis pipeline for resume id: {}", resumeId);

        Resume resume = resumeRepository.findById(resumeId).orElse(null);
        if (resume == null) {
            log.error("Resume not found with id: {}. Skipping analysis.", resumeId);
            return;
        }

        try {
            resume.markAsProcessing();
            resumeRepository.save(resume);

            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(extractedText);

            // Execute generic prompt on AiClient with typed target DTO
            ResumeAnalysisResult dto = aiClient.sendPrompt(systemPrompt, userPrompt, ResumeAnalysisResult.class);

            // Create domain analysis object binding system metadata (resumeId, timestamp) with AI response
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
            resume.failAnalysis(e.getMessage());
            resumeRepository.save(resume);
        }
    }
}
