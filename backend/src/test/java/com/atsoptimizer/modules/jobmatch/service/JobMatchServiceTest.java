package com.atsoptimizer.modules.jobmatch.service;

import com.atsoptimizer.infra.message.JobMatchEventPublisher;
import com.atsoptimizer.infra.message.dto.JobMatchGenerationMessage;
import com.atsoptimizer.infra.message.dto.JobMatchMessage;
import com.atsoptimizer.modules.jobmatch.domain.JobFitAudit;
import com.atsoptimizer.modules.jobmatch.domain.JobMatch;
import com.atsoptimizer.modules.jobmatch.domain.JobMatchStatus;
import com.atsoptimizer.modules.jobmatch.domain.JobRequirements;
import com.atsoptimizer.modules.jobmatch.domain.KeywordMatchReport;
import com.atsoptimizer.modules.jobmatch.domain.OptimizationMode;
import com.atsoptimizer.modules.jobmatch.domain.StructuredResumeTemplateData;
import com.atsoptimizer.modules.jobmatch.dto.CreateJobMatchRequest;
import com.atsoptimizer.modules.jobmatch.dto.JobMatchResponseDto;
import com.atsoptimizer.modules.jobmatch.exception.EmptyJobDescriptionException;
import com.atsoptimizer.modules.jobmatch.exception.GeneratedResumeNotFoundException;
import com.atsoptimizer.modules.jobmatch.exception.JobMatchNotFoundException;
import com.atsoptimizer.modules.jobmatch.exception.JobMatchNotReadyForGenerationException;
import com.atsoptimizer.modules.jobmatch.exception.ResumeNotReadyForMatchException;
import com.atsoptimizer.modules.jobmatch.repository.JobMatchRepository;
import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.ResumeAnalysis;
import com.atsoptimizer.modules.resume.exception.ResumeNotFoundException;
import com.atsoptimizer.modules.resume.repository.ResumeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMatchServiceTest {

    @Mock
    private JobMatchRepository jobMatchRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private JobMatchEventPublisher jobMatchEventPublisher;

    @Mock
    private ResumePdfGeneratorService resumePdfGeneratorService;

    @InjectMocks
    private JobMatchService jobMatchService;

    @Test
    @DisplayName("Should create job match and publish to PARSE queue when resume is COMPLETED")
    void shouldCreateJobMatchSuccessfully() {
        Resume resume = completedResume(1L);
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));

        JobMatch saved = new JobMatch(10L, resume, "Vaga Java Spring Boot");
        when(jobMatchRepository.save(any(JobMatch.class))).thenReturn(saved);

        JobMatchResponseDto response = jobMatchService.createJobMatch(new CreateJobMatchRequest(1L, "  Vaga Java Spring Boot  "));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.resumeId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(JobMatchStatus.PENDING_ANALYSIS);

        ArgumentCaptor<JobMatchMessage> captor = ArgumentCaptor.forClass(JobMatchMessage.class);
        verify(jobMatchEventPublisher).publishForJobParsing(captor.capture());
        assertThat(captor.getValue().jobMatchId()).isEqualTo(10L);
        assertThat(captor.getValue().resumeId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw EmptyJobDescriptionException when job description is blank")
    void shouldThrowWhenJobDescriptionBlank() {
        assertThatThrownBy(() -> jobMatchService.createJobMatch(new CreateJobMatchRequest(1L, "   ")))
                .isInstanceOf(EmptyJobDescriptionException.class);
    }

    @Test
    @DisplayName("Should throw ResumeNotFoundException when creating match for missing resume")
    void shouldThrowWhenResumeNotFound() {
        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobMatchService.createJobMatch(new CreateJobMatchRequest(999L, "Vaga Java")))
                .isInstanceOf(ResumeNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ResumeNotReadyForMatchException when resume has not completed initial analysis")
    void shouldThrowWhenResumeNotCompleted() {
        Resume uploadedOnly = new Resume(1L, "cv.pdf", "application/pdf", 500L, "Texto");
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(uploadedOnly));

        assertThatThrownBy(() -> jobMatchService.createJobMatch(new CreateJobMatchRequest(1L, "Vaga Java")))
                .isInstanceOf(ResumeNotReadyForMatchException.class);
    }

    @Test
    @DisplayName("Should queue resume generation when job match is ANALYZED")
    void shouldQueueResumeGenerationWhenAnalyzed() {
        Resume resume = completedResume(1L);
        JobMatch match = analyzedJobMatch(10L, resume);
        when(jobMatchRepository.findWithResumeById(10L)).thenReturn(Optional.of(match));
        when(jobMatchRepository.save(match)).thenReturn(match);

        JobMatchResponseDto response = jobMatchService.requestResumeGeneration(10L, OptimizationMode.TRUTHFUL);

        assertThat(response.status()).isEqualTo(JobMatchStatus.GENERATING_RESUME);
        verify(jobMatchEventPublisher).publishForResumeGeneration(new JobMatchGenerationMessage(10L, OptimizationMode.TRUTHFUL));
    }

    @Test
    @DisplayName("Should throw JobMatchNotReadyForGenerationException when initial match audit is not complete")
    void shouldThrowWhenNotReadyForGeneration() {
        JobMatch pending = new JobMatch(10L, completedResume(1L), "Vaga Java");
        when(jobMatchRepository.findWithResumeById(10L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> jobMatchService.requestResumeGeneration(10L, OptimizationMode.IDEAL))
                .isInstanceOf(JobMatchNotReadyForGenerationException.class);
    }

    @Test
    @DisplayName("Should return generated PDF binary and filename for requested mode")
    void shouldDownloadGeneratedPdf() {
        JobMatch match = analyzedJobMatch(10L, completedResume(1L));
        StructuredResumeTemplateData data = new StructuredResumeTemplateData(
                "Lucas_Backend_Java.pdf", "Lucas", "Recife", "81999", "a@b.com",
                "linkedin", List.of(), "DEV JAVA", "Resumo", List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        match.completeResumeGeneration(OptimizationMode.TRUTHFUL, data);

        when(jobMatchRepository.findWithResumeById(10L)).thenReturn(Optional.of(match));
        when(resumePdfGeneratorService.generatePdf(data)).thenReturn(new byte[]{1, 2, 3});

        JobMatchService.GeneratedPdfFile pdfFile = jobMatchService.downloadGeneratedResumePdf(10L, OptimizationMode.TRUTHFUL);

        assertThat(pdfFile.fileName()).isEqualTo("Lucas_Backend_Java.pdf");
        assertThat(pdfFile.content()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Should throw GeneratedResumeNotFoundException when requesting PDF for mode not yet generated")
    void shouldThrowWhenModeNotGeneratedYet() {
        JobMatch match = analyzedJobMatch(10L, completedResume(1L));
        when(jobMatchRepository.findWithResumeById(10L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> jobMatchService.downloadGeneratedResumePdf(10L, OptimizationMode.IDEAL))
                .isInstanceOf(GeneratedResumeNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw JobMatchNotFoundException when ID does not exist")
    void shouldThrowWhenJobMatchNotFound() {
        when(jobMatchRepository.findWithResumeById(777L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobMatchService.getJobMatchById(777L))
                .isInstanceOf(JobMatchNotFoundException.class);
    }

    private Resume completedResume(Long id) {
        Resume resume = new Resume(id, "cv.pdf", "application/pdf", 1000L, "Texto");
        resume.completeAnalysis(new ResumeAnalysis(
                id, 85, "Lucas", "Dev", null, "About", List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), "Obj", "Prof", "Struct", "ATS",
                List.of("Java"), List.of(), List.of(), List.of(), List.of(), List.of(), LocalDateTime.now()
        ));
        return resume;
    }

    private JobMatch analyzedJobMatch(Long id, Resume resume) {
        JobMatch match = new JobMatch(id, resume, "Vaga Java Spring");
        match.completeJobParsing(
                new JobRequirements("Dev Java", "Júnior", List.of(), List.of(), List.of(), List.of(), List.of()),
                new KeywordMatchReport(100, 100, List.of("Java"), List.of(), List.of(), List.of())
        );
        match.completeFitAudit(new JobFitAudit(85, 80, 85, "Ótimo match", List.of("Java presente"), List.of(), List.of()));
        return match;
    }
}
