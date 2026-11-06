package com.atsoptimizer.infra.ai;

import com.atsoptimizer.infra.ai.dto.ContactInfoDto;
import com.atsoptimizer.infra.ai.dto.ResumeAnalysisResult;
import com.atsoptimizer.infra.ai.exception.AiProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisAITest {

    @Mock
    private AiClient aiClient;

    @InjectMocks
    private ResumeAnalysisAI resumeAnalysisAi;

    @Test
    @DisplayName("Should build appropriate prompts and delegate to AiClient with target DTO class")
    void shouldBuildPromptsAndDelegateToAiClient() {
        String resumeText = "Lucas Ferreira - Desenvolvedor Backend Java Spring";
        ContactInfoDto contactInfo = new ContactInfoDto("11999999999", "lucas@example.com", "SP", "linkedin.com/in/lucas", List.of());
        ResumeAnalysisResult expectedResult = new ResumeAnalysisResult(
                "Lucas Ferreira",
                "Desenvolvedor Backend Java",
                contactInfo,
                "Resumo profissional",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "Desenvolvedor Backend",
                "Perfil forte",
                "Formatação compatível",
                "Excelente compatibilidade",
                List.of("Java", "Spring"),
                List.of("AWS"),
                List.of(),
                List.of("Experiência consistente"),
                List.of(),
                List.of("Adicionar métricas"),
                90
        );

        when(aiClient.sendPrompt(anyString(), anyString(), eq(ResumeAnalysisResult.class)))
                .thenReturn(expectedResult);

        ResumeAnalysisResult actualResult = resumeAnalysisAi.sendToAiAnalisysResume(resumeText);

        assertThat(actualResult).isNotNull();
        assertThat(actualResult.candidateName()).isEqualTo("Lucas Ferreira");
        assertThat(actualResult.headline()).isEqualTo("Desenvolvedor Backend Java");
        assertThat(actualResult.score()).isEqualTo(90);
        assertThat(actualResult.inferredObjective()).isEqualTo("Desenvolvedor Backend");

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);

        verify(aiClient).sendPrompt(systemPromptCaptor.capture(), userPromptCaptor.capture(), eq(ResumeAnalysisResult.class));

        assertThat(systemPromptCaptor.getValue()).contains("Applicant Tracking Systems", "Tech Recruiter", "EXTRAÇÃO ESTRUTURADA DE DADOS");
        assertThat(systemPromptCaptor.getValue()).contains("1 PÁGINA", "ZERO ALUCINAÇÃO");
        assertThat(userPromptCaptor.getValue()).contains(resumeText, "--- INÍCIO DO CURRÍCULO ---");
    }

    @Test
    @DisplayName("Should propagate AiProcessingException when AiClient fails")
    void shouldPropagateExceptionWhenAiClientFails() {
        when(aiClient.sendPrompt(anyString(), anyString(), any()))
                .thenThrow(new AiProcessingException("Rate limit reached"));

        assertThatThrownBy(() -> resumeAnalysisAi.sendToAiAnalisysResume("Texto qualquer"))
                .isInstanceOf(AiProcessingException.class)
                .hasMessageContaining("Rate limit reached");
    }
}
