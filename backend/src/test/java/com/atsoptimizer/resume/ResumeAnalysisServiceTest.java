package com.atsoptimizer.resume;

import com.atsoptimizer.ai.AiClient;
import com.atsoptimizer.ai.ResumeAnalysisPromptBuilder;
import com.atsoptimizer.ai.dto.ResumeAnalysisResult;
import com.atsoptimizer.resume.domain.Resume;
import com.atsoptimizer.resume.domain.ResumeStatus;
import com.atsoptimizer.resume.repository.ResumeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private AiClient aiClient;

    @Mock
    private ResumeAnalysisPromptBuilder promptBuilder;

    @InjectMocks
    private ResumeAnalysisService resumeAnalysisService;

    @Test
    @DisplayName("Should successfully execute AI prompt and persist rich analysis with COMPLETED status and resumeId")
    void shouldProcessAnalysisSuccessfully() {
        Long resumeId = 1L;
        Resume resume = new Resume(resumeId, "curriculo.pdf", "application/pdf", 1024L, "Texto do curriculo");

        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));
        when(promptBuilder.buildSystemPrompt()).thenReturn("System prompt");
        when(promptBuilder.buildUserPrompt(anyString())).thenReturn("User prompt");

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

        when(aiClient.sendPrompt(anyString(), anyString(), eq(ResumeAnalysisResult.class)))
                .thenReturn(aiResult);

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
    @DisplayName("Should mark resume as FAILED through semantic failAnalysis when AI client throws exception")
    void shouldHandleAiFailureGracefully() {
        Long resumeId = 1L;
        Resume resume = new Resume(resumeId, "curriculo.pdf", "application/pdf", 1024L, "Texto do curriculo");

        when(resumeRepository.findById(resumeId)).thenReturn(Optional.of(resume));
        when(promptBuilder.buildSystemPrompt()).thenReturn("System prompt");
        when(promptBuilder.buildUserPrompt(anyString())).thenReturn("User prompt");

        when(aiClient.sendPrompt(anyString(), anyString(), eq(ResumeAnalysisResult.class)))
                .thenThrow(new RuntimeException("OpenAI API timeout"));

        resumeAnalysisService.processAnalysis(resumeId, "Texto do curriculo");

        assertThat(resume.getStatus()).isEqualTo(ResumeStatus.FAILED);
        assertThat(resume.getErrorMessage()).contains("OpenAI API timeout");

        verify(resumeRepository, times(2)).save(resume);
    }
}
