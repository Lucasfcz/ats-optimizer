package com.atsoptimizer.ai;

import com.atsoptimizer.ai.dto.ResumeAnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiClientIntegrationTest {

    private ChatModel chatModel;
    private AiClient aiClient;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        aiClient = new AiClient(builder);
    }

    @Test
    @DisplayName("Should integrate with Spring AI ChatClient to execute prompt and deserialize ResumeAnalysisResult")
    void shouldExecutePromptAndDeserializeResumeAnalysisResult() {
        String systemInstruction = "Você é um avaliador técnico de currículos para sistemas ATS.";
        String userContent = "Currículo com 10 anos de experiência em Java e Spring Boot.";

        String simulatedAiJsonResponse = """
                {
                  "score": 92,
                  "inferredObjective": "Especialista Backend Java",
                  "overallProfile": "Profissional sênior com vasta bagagem técnica.",
                  "structureEvaluation": "Estrutura excelente com clara divisão de seções.",
                  "atsCompatibilitySummary": "Alta probabilidade de aprovação nos robôs ATS.",
                  "keywords": ["Java", "Spring Boot", "Docker", "PostgreSQL"],
                  "missingKeywords": ["AWS", "Kubernetes"],
                  "inconsistencies": [],
                  "strengths": ["Experiência sólida", "Stack moderna"],
                  "weaknesses": ["Pouca ênfase em cloud"],
                  "improvementSuggestions": ["Destacar experiências com provedores de nuvem"]
                }
                """;

        Generation generation = new Generation(new AssistantMessage(simulatedAiJsonResponse));
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        ResumeAnalysisResult result = aiClient.sendPrompt(systemInstruction, userContent, ResumeAnalysisResult.class);

        assertThat(result).isNotNull();
        assertThat(result.score()).isEqualTo(92);
        assertThat(result.inferredObjective()).isEqualTo("Especialista Backend Java");
        assertThat(result.keywords()).containsExactly("Java", "Spring Boot", "Docker", "PostgreSQL");
        assertThat(result.missingKeywords()).containsExactly("AWS", "Kubernetes");
        assertThat(result.strengths()).contains("Experiência sólida");
        assertThat(result.weaknesses()).contains("Pouca ênfase em cloud");
        assertThat(result.improvementSuggestions()).contains("Destacar experiências com provedores de nuvem");

        // Verify that the prompt sent to ChatModel contains system text and user text
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        Prompt executedPrompt = promptCaptor.getValue();
        assertThat(executedPrompt.getContents()).contains(userContent);
    }

    // Record auxiliar para provar que AiClient.sendPrompt aceita qualquer classe genérica <T>
    record CustomReviewDto(String decision, int confidence, List<String> tags) {}

    @Test
    @DisplayName("Should demonstrate generic <T> typing with a different target entity class")
    void shouldExecutePromptWithDifferentGenericEntity() {
        String systemInstruction = "Você é um classificador de documentos.";
        String userContent = "Classifique este texto.";

        String simulatedAiJsonResponse = """
                {
                  "decision": "APPROVED",
                  "confidence": 98,
                  "tags": ["TECH", "JAVA"]
                }
                """;

        Generation generation = new Generation(new AssistantMessage(simulatedAiJsonResponse));
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        CustomReviewDto review = aiClient.sendPrompt(systemInstruction, userContent, CustomReviewDto.class);

        assertThat(review).isNotNull();
        assertThat(review.decision()).isEqualTo("APPROVED");
        assertThat(review.confidence()).isEqualTo(98);
        assertThat(review.tags()).containsExactly("TECH", "JAVA");
    }
}
