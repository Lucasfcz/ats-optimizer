package com.atsoptimizer.modules.resume;

import com.atsoptimizer.infra.ai.ResumeAnalysisAI;
import com.atsoptimizer.infra.ai.dto.ResumeAnalysisResult;
import com.atsoptimizer.infra.ai.exception.AiProcessingException;
import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.ResumeStatus;
import com.atsoptimizer.modules.resume.exception.ResumeNotFoundException;
import com.atsoptimizer.modules.resume.repository.ResumeRepository;
import com.atsoptimizer.modules.resume.service.ResumeAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ResumeAnalysisAI resumeAnalysisAi;

    @InjectMocks
    private ResumeAnalysisService resumeAnalysisService;

    @Test
    @DisplayName("Should successfully execute AI prompt and persist rich analysis with COMPLETED status and resumeId")
    void shouldProcessAnalysisSuccessfully() {
        Long resumeId = 1L;
        Resume resume = new Resume(resumeId, "curriculo.pdf", "application/pdf", 1024L, "Texto do curriculo");

        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));

        ResumeAnalysisResult aiResult = new ResumeAnalysisResult(
                List.of("Java", "Spring Boot", "PostgreSQL"),
                List.of("Docker", "Kubernetes"),
                "Desenvolvedor Backend Sênior",
                "Perfil sólido com forte aderência técnica",
                "Estrutura clara com títulos adequados para ATS",
                List.of(),
                List.of("Experiência relevante", "Boas práticas"),
                List.of("Falta de métricas de impacto"),
                List.of("Adicionar dados quantitativos de resultados"),
                88,
                "Currículo altamente compatível com filtros ATS modernos"
        );

        when(resumeAnalysisAi.sendToAiAnalisysResume(anyString())).thenReturn(aiResult);

        resumeAnalysisService.processAnalysis(resumeId, "Texto do curriculo");

        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.COMPLETED);
        assertThat(resume.getAnalysis()).isNotNull();
        assertThat(resume.getAnalysis().getResumeId()).isEqualTo(resumeId);
        assertThat(resume.getAnalysis().getScore()).isEqualTo(88);
        assertThat(resume.getAnalysis().getInferredObjective()).isEqualTo("Desenvolvedor Backend Sênior");
        assertThat(resume.getAnalysis().getKeywords()).contains("Java", "Spring Boot");
        assertThat(resume.getErrorMessage()).isNull();

        verify(resumeRepository, times(2)).save(resume);
    }

    @Test
    @DisplayName("Should rethrow AiProcessingException when AI client fails so Spring AMQP can retry and DLQ")
    void shouldRethrowAiProcessingExceptionWhenAiFails() {
        Long resumeId = 1L;
        Resume resume = new Resume(resumeId, "curriculo.pdf", "application/pdf", 1024L, "Texto do curriculo");

        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));

        when(resumeAnalysisAi.sendToAiAnalisysResume(anyString()))
                .thenThrow(new AiProcessingException("OpenAI API timeout"));

        assertThatThrownBy(() -> resumeAnalysisService.processAnalysis(resumeId, "Texto do curriculo"))
                .isInstanceOf(AiProcessingException.class)
                .hasMessageContaining("OpenAI API timeout");

        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.PROCESSING);
        verify(resumeRepository, times(1)).save(resume);
    }

    @Test
    @DisplayName("Should throw ResumeNotFoundException when resume ID does not exist in database")
    void shouldThrowResumeNotFoundExceptionWhenResumeDoesNotExist() {
        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeAnalysisService.processAnalysis(999L, "Texto"))
                .isInstanceOf(ResumeNotFoundException.class)
                .hasMessage("Resume not found with id: 999");
    }
}
