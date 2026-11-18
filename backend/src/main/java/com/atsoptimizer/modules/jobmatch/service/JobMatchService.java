package com.atsoptimizer.modules.jobmatch.service;

import com.atsoptimizer.infra.message.JobMatchEventPublisher;
import com.atsoptimizer.infra.message.dto.JobMatchGenerationMessage;
import com.atsoptimizer.infra.message.dto.JobMatchMessage;
import com.atsoptimizer.modules.jobmatch.domain.JobMatch;
import com.atsoptimizer.modules.jobmatch.domain.OptimizationMode;
import com.atsoptimizer.modules.jobmatch.domain.StructuredResumeTemplateData;
import com.atsoptimizer.modules.jobmatch.dto.CreateJobMatchRequest;
import com.atsoptimizer.modules.jobmatch.dto.JobMatchDetailsDto;
import com.atsoptimizer.modules.jobmatch.dto.JobMatchResponseDto;
import com.atsoptimizer.modules.jobmatch.exception.EmptyJobDescriptionException;
import com.atsoptimizer.modules.jobmatch.exception.JobMatchNotFoundException;
import com.atsoptimizer.modules.jobmatch.exception.ResumeNotReadyForMatchException;
import com.atsoptimizer.modules.jobmatch.repository.JobMatchRepository;
import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.ResumeStatus;
import com.atsoptimizer.modules.resume.exception.ResumeNotFoundException;
import com.atsoptimizer.modules.resume.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobMatchService {

    private static final Logger log = LoggerFactory.getLogger(JobMatchService.class);

    public record GeneratedPdfFile(String fileName, byte[] content) {
    }

    private final JobMatchRepository jobMatchRepository;
    private final ResumeRepository resumeRepository;
    private final JobMatchEventPublisher jobMatchEventPublisher;
    private final ResumePdfGeneratorService resumePdfGeneratorService;

    public JobMatchService(
            JobMatchRepository jobMatchRepository,
            ResumeRepository resumeRepository,
            JobMatchEventPublisher jobMatchEventPublisher,
            ResumePdfGeneratorService resumePdfGeneratorService
    ) {
        this.jobMatchRepository = jobMatchRepository;
        this.resumeRepository = resumeRepository;
        this.jobMatchEventPublisher = jobMatchEventPublisher;
        this.resumePdfGeneratorService = resumePdfGeneratorService;
    }

    @Transactional
    public JobMatchResponseDto createJobMatch(CreateJobMatchRequest request) {
        if (request.jobDescription() == null || request.jobDescription().isBlank()) {
            throw new EmptyJobDescriptionException();
        }

        Resume resume = resumeRepository.findById(request.resumeId())
                .orElseThrow(() -> new ResumeNotFoundException(request.resumeId()));

        if (resume.getStatus() != ResumeStatus.COMPLETED || resume.getAnalysis() == null) {
            throw new ResumeNotReadyForMatchException(resume.getId(), resume.getStatus());
        }

        JobMatch jobMatch = new JobMatch(resume, request.jobDescription().trim());
        JobMatch saved = jobMatchRepository.save(jobMatch);
        log.info("Created jobMatch [{}] for resume [{}]", saved.getId(), resume.getId());

        jobMatchEventPublisher.publishForJobParsing(new JobMatchMessage(saved.getId(), resume.getId()));
        return JobMatchResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public JobMatchDetailsDto getJobMatchById(Long id) {
        JobMatch jobMatch = jobMatchRepository.findWithResumeById(id)
                .orElseThrow(() -> new JobMatchNotFoundException(id));
        return JobMatchDetailsDto.fromEntity(jobMatch);
    }

    @Transactional(readOnly = true)
    public List<JobMatchResponseDto> getJobMatchesByResumeId(Long resumeId) {
        if (!resumeRepository.existsById(resumeId)) {
            throw new ResumeNotFoundException(resumeId);
        }
        return jobMatchRepository.findAllByResumeIdWithResume(resumeId)
                .stream()
                .map(JobMatchResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    public JobMatchResponseDto requestResumeGeneration(Long jobMatchId, OptimizationMode mode) {
        JobMatch jobMatch = jobMatchRepository.findWithResumeById(jobMatchId)
                .orElseThrow(() -> new JobMatchNotFoundException(jobMatchId));

        jobMatch.markAsGeneratingResume();
        JobMatch saved = jobMatchRepository.save(jobMatch);

        log.info("Queued [{}] resume generation for jobMatch [{}]", mode, jobMatchId);
        jobMatchEventPublisher.publishForResumeGeneration(new JobMatchGenerationMessage(jobMatchId, mode));

        return JobMatchResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public GeneratedPdfFile downloadGeneratedResumePdf(Long jobMatchId, OptimizationMode mode) {
        JobMatch jobMatch = jobMatchRepository.findWithResumeById(jobMatchId)
                .orElseThrow(() -> new JobMatchNotFoundException(jobMatchId));

        StructuredResumeTemplateData templateData = jobMatch.requireGeneratedResumeData(mode);
        byte[] pdfBytes = resumePdfGeneratorService.generatePdf(templateData);

        return new GeneratedPdfFile(templateData.suggestedFileName(), pdfBytes);
    }
}
