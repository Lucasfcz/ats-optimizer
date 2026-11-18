package com.atsoptimizer.modules.jobmatch.service;

import com.atsoptimizer.infra.ai.IdealResumeFormatterAI;
import com.atsoptimizer.infra.ai.JobDescriptionParserAI;
import com.atsoptimizer.infra.ai.JobFitAuditAI;
import com.atsoptimizer.infra.ai.TruthfulResumeFormatterAI;
import com.atsoptimizer.infra.ai.dto.JobFitAuditAiResult;
import com.atsoptimizer.infra.ai.dto.JobRequirementsAiResult;
import com.atsoptimizer.infra.ai.dto.JobSkillRequirementDto;
import com.atsoptimizer.infra.ai.dto.SkillCategoryDto;
import com.atsoptimizer.infra.ai.dto.StructuredResumeAiResult;
import com.atsoptimizer.infra.ai.exception.AiProcessingException;
import com.atsoptimizer.infra.message.JobMatchEventPublisher;
import com.atsoptimizer.infra.message.dto.JobMatchMessage;
import com.atsoptimizer.modules.jobmatch.domain.JobFitAudit;
import com.atsoptimizer.modules.jobmatch.domain.JobMatch;
import com.atsoptimizer.modules.jobmatch.domain.JobMatchStatus;
import com.atsoptimizer.modules.jobmatch.domain.JobRequirements;
import com.atsoptimizer.modules.jobmatch.domain.KeywordMatchReport;
import com.atsoptimizer.modules.jobmatch.domain.OptimizationMode;
import com.atsoptimizer.modules.jobmatch.domain.StructuredResumeTemplateData;
import com.atsoptimizer.modules.jobmatch.repository.JobMatchRepository;
import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.ResumeAnalysis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMatchProcessingServiceTest {

    @Mock
    private JobMatchRepository jobMatchRepository;

    @Mock
    private JobDescriptionParserAI jobDescriptionParserAI;

    @Mock
    private JobFitAuditAI jobFitAuditAI;

    @Mock
    private TruthfulResumeFormatterAI truthfulResumeFormatterAI;

    @Mock
    private IdealResumeFormatterAI idealResumeFormatterAI;

    @Mock
    private KeywordMatcher keywordMatcher;

    @Mock
    private ResumePdfGeneratorService resumePdfGeneratorService;

    @Mock
    private JobMatchEventPublisher jobMatchEventPublisher;

    @InjectMocks
    private JobMatchProcessingService processingService;

    @Test
    @DisplayName("Stage 1: should parse job requirements, run KeywordMatcher, save report and publish to AUDIT queue")
    void shouldProcessJobParsingAndPublishToAudit() {
        Resume resume = completedResume(1L);
        JobMatch jobMatch = new JobMatch(10L, resume, "Vaga Java Spring Boot + PostgreSQL");
        when(jobMatchRepository.findWithResumeById(10L)).thenReturn(Optional.of(jobMatch));

        JobRequirementsAiResult aiRequirements = new JobRequirementsAiResult(
                "Backend Java", "Júnior",
                List.of(new JobSkillRequirementDto("Java", List.of())),
                List.of(new JobSkillRequirementDto("Docker", List.of())),
                List.of("Trabalho em equipe"),
                List.of("Construir APIs REST"),
                List.of("SOLID")
        );
        when(jobDescriptionParserAI.extractRequirements(anyString())).thenReturn(aiRequirements);

        KeywordMatchReport report = new KeywordMatchReport(100, 0, List.of("Java"), List.of(), List.of(), List.of("Docker"));
        when(keywordMatcher.evaluate(any(Resume.class), any(JobRequirements.class))).thenReturn(report);

        processingService.processJobParsing(10L);

        assertThat(jobMatch.getStatus()).isEqualTo(JobMatchStatus.AUDITING_FIT);
        assertThat(jobMatch.getJobRequirements().jobTitle()).isEqualTo("Backend Java");
        assertThat(jobMatch.getKeywordMatchReport().requiredSkillsMatchPercentage()).isEqualTo(100);
        verify(jobMatchEventPublisher).publishForFitAudit(new JobMatchMessage(10L, 1L));
    }

    @Test
    @DisplayName("Stage 2: should run semantic audit, compute weighted composite score and transition to ANALYZED")
    void shouldProcessFitAuditWithCompositeScore() {
        Resume resume = completedResume(1L);
        JobMatch jobMatch = new JobMatch(10L, resume, "Vaga Java Spring");
        jobMatch.completeJobParsing(
                new JobRequirements("Backend Java", "Júnior", List.of(), List.of(), List.of(), List.of(), List.of()),
                new KeywordMatchReport(80, 50, List.of("Java"), List.of("Spring"), List.of(), List.of("Kafka"))
        );
        when(jobMatchRepository.findWithResumeById(10L)).thenReturn(Optional.of(jobMatch));

        // Weighted: (80 * 0.50) + (50 * 0.15) + (90 * 0.25) + (70 * 0.10) = 40 + 7.5 + 22.5 + 7 = 77
        JobFitAuditAiResult aiAudit = new JobFitAuditAiResult(
                90, 70, "Bom alinhamento com base Java",
                List.of("Sólida base em Java"), List.of("Falta Kafka"), List.of("Destacar Spring nos projetos")
        );
        when(jobFitAuditAI.evaluateFit(any(), any(), any(), any(), any())).thenReturn(aiAudit);

        processingService.processFitAudit(10L);

        assertThat(jobMatch.getStatus()).isEqualTo(JobMatchStatus.ANALYZED);
        assertThat(jobMatch.getFitAudit().finalMatchScore()).isEqualTo(77);
        assertThat(jobMatch.getFitAudit().matchReasons()).contains("Sólida base em Java");
    }

    @Test
    @DisplayName("Stage 3 (TRUTHFUL): should strip injected gaps, sanitize file name and skip condense when fitting 1 page")
    void shouldGenerateTruthfulResumeWithoutCondenseWhenFitting() {
        JobMatch jobMatch = analyzedMatch(10L);
        when(jobMatchRepository.findWithResumeById(10L)).thenReturn(Optional.of(jobMatch));

        StructuredResumeAiResult aiResult = new StructuredResumeAiResult(
                "Lucas Cabral - Java Backend",
                "Lucas Cabral", "Recife, PE", "81999", "a@b.com", "linkedin.com/in/lucas",
                List.of("github.com/lucas"), "DESENVOLVEDOR JAVA", "Resumo",
                List.of(new SkillCategoryDto("Linguagens", List.of("Java"))),
                List.of(), List.of(), List.of(), List.of(),
                List.of("Should be stripped in truthful mode")
        );
        when(truthfulResumeFormatterAI.generate(any(), any(), any(), any(), any())).thenReturn(aiResult);
        when(resumePdfGeneratorService.fitsInSinglePage(any())).thenReturn(true);
        when(resumePdfGeneratorService.pruneToSinglePageIfNeeded(any())).thenAnswer(inv -> inv.getArgument(0));

        processingService.processResumeGeneration(10L, OptimizationMode.TRUTHFUL);

        assertThat(jobMatch.getStatus()).isEqualTo(JobMatchStatus.COMPLETED);
        StructuredResumeTemplateData saved = jobMatch.getTruthfulResumeData();
        assertThat(saved.suggestedFileName()).isEqualTo("Lucas_Cabral_-_Java_Backend.pdf");
        assertThat(saved.injectedGaps()).isEmpty();
        verify(truthfulResumeFormatterAI, never()).condenseToSinglePage(any());
    }

    @Test
    @DisplayName("Stage 3 (IDEAL + Condense Pass): should trigger AI condense when oversized and preserve injected gaps")
    void shouldTriggerCondensePassWhenOversizedInIdealMode() {
        JobMatch jobMatch = analyzedMatch(10L);
        when(jobMatchRepository.findWithResumeById(10L)).thenReturn(Optional.of(jobMatch));

        StructuredResumeAiResult initial = new StructuredResumeAiResult(
                "Lucas_Backend.pdf", "Lucas", "PE", "81", "a@b.com", "li", List.of(),
                "DEV JAVA", "Resumo longo", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of("Kafka", "Kubernetes")
        );
        StructuredResumeAiResult condensed = new StructuredResumeAiResult(
                "Lucas_Backend.pdf", "Lucas", "PE", "81", "a@b.com", "li", List.of(),
                "DEV JAVA", "Resumo conciso", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of("Kafka", "Kubernetes")
        );

        when(idealResumeFormatterAI.generate(any(), any(), any(), any(), any())).thenReturn(initial);
        when(resumePdfGeneratorService.fitsInSinglePage(any())).thenReturn(false);
        when(idealResumeFormatterAI.condenseToSinglePage(any())).thenReturn(condensed);
        when(resumePdfGeneratorService.pruneToSinglePageIfNeeded(any())).thenAnswer(inv -> inv.getArgument(0));

        processingService.processResumeGeneration(10L, OptimizationMode.IDEAL);

        assertThat(jobMatch.getStatus()).isEqualTo(JobMatchStatus.COMPLETED);
        assertThat(jobMatch.getIdealResumeData().summary()).isEqualTo("Resumo conciso");
        assertThat(jobMatch.getIdealResumeData().injectedGaps()).containsExactly("Kafka", "Kubernetes");
        verify(idealResumeFormatterAI).condenseToSinglePage(any());
        verify(resumePdfGeneratorService).pruneToSinglePageIfNeeded(any());
    }

    @Test
    @DisplayName("Should rethrow AiProcessingException for RabbitMQ retry and DLQ recovery")
    void shouldRethrowAiProcessingException() {
        JobMatch jobMatch = new JobMatch(10L, completedResume(1L), "Vaga Java");
        when(jobMatchRepository.findWithResumeById(10L)).thenReturn(Optional.of(jobMatch));
        when(jobDescriptionParserAI.extractRequirements(anyString()))
                .thenThrow(new AiProcessingException("Gemini 429 quota"));

        assertThatThrownBy(() -> processingService.processJobParsing(10L))
                .isInstanceOf(AiProcessingException.class);
    }

    private Resume completedResume(Long id) {
        Resume resume = new Resume(id, "cv.pdf", "application/pdf", 1000L, "Texto Java");
        resume.completeAnalysis(new ResumeAnalysis(
                id, 85, "Lucas", "Dev Java", null, "About", List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), "Obj", "Prof", "Struct", "ATS",
                List.of("Java"), List.of(), List.of(), List.of(), List.of(), List.of(), LocalDateTime.now()
        ));
        return resume;
    }

    private JobMatch analyzedMatch(Long id) {
        JobMatch jobMatch = new JobMatch(id, completedResume(1L), "Vaga Java");
        jobMatch.completeJobParsing(
                new JobRequirements("Backend Java", "Júnior", List.of(), List.of(), List.of(), List.of(), List.of()),
                new KeywordMatchReport(100, 100, List.of("Java"), List.of(), List.of(), List.of())
        );
        jobMatch.completeFitAudit(new JobFitAudit(90, 88, 90, "Excelente", List.of(), List.of(), List.of()));
        return jobMatch;
    }
}
