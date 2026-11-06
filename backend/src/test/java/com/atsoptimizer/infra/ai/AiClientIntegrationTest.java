package com.atsoptimizer.infra.ai;

import com.atsoptimizer.infra.ai.dto.ContactInfoDto;
import com.atsoptimizer.infra.ai.dto.ResumeAnalysisResult;
import com.atsoptimizer.infra.ai.exception.AiProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiClientIntegrationTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder builder;

    private AiClient aiClient;

    @BeforeEach
    void setUp() {
        aiClient = new AiClient(builder);
    }

    @Test
    @DisplayName("Should successfully bind generic class type to entity response from ChatClient")
    void shouldSuccessfullySendPromptWithStructuredOutput() {
        ContactInfoDto contact = new ContactInfoDto("11999999999", "dev@example.com", "São Paulo", "linkedin.com/in/dev", List.of());
        ResumeAnalysisResult expected = new ResumeAnalysisResult(
                "Jane Doe",
                "Senior Backend Engineer",
                contact,
                "Experienced backend software engineer",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "Backend Engineer",
                "Perfil forte",
                "Boa estrutura",
                "Alta compatibilidade",
                List.of("Java", "Spring Boot"),
                List.of("Docker"),
                List.of(),
                List.of("Clareza"),
                List.of("Métricas"),
                List.of("Adicionar mais dados"),
                85
        );

        ChatClient mockChatClient = builder.build();
        when(mockChatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .entity(ResumeAnalysisResult.class))
                .thenReturn(expected);

        ResumeAnalysisResult result = aiClient.sendPrompt(
                "System instructions",
                "User prompt with resume text",
                ResumeAnalysisResult.class
        );

        assertThat(result).isNotNull();
        assertThat(result.candidateName()).isEqualTo("Jane Doe");
        assertThat(result.headline()).isEqualTo("Senior Backend Engineer");
        assertThat(result.score()).isEqualTo(85);
        assertThat(result.inferredObjective()).isEqualTo("Backend Engineer");
        assertThat(result.keywords()).containsExactly("Java", "Spring Boot");
    }

    @Test
    @DisplayName("Should wrap and throw AiProcessingException when ChatClient fails")
    void shouldThrowAiProcessingExceptionWhenChatClientFails() {
        ChatClient mockChatClient = builder.build();
        when(mockChatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call())
                .thenThrow(new RuntimeException("API Connection Refused"));

        assertThatThrownBy(() -> aiClient.sendPrompt(
                "System instructions",
                "User text",
                ResumeAnalysisResult.class
        ))
                .isInstanceOf(AiProcessingException.class)
                .hasMessageContaining("AI processing failed for entity ResumeAnalysisResult")
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
