package com.atsoptimizer.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record ResumeAnalysisResult(
        @JsonPropertyDescription("Principais palavras-chave encontradas no currículo")
        List<String> keywords,

        @JsonPropertyDescription("Palavras-chave essenciais faltantes para sistemas ATS")
        List<String> missingKeywords,

        @JsonPropertyDescription("Objetivo profissional inferido a partir das experiências e formação")
        String inferredObjective,

        @JsonPropertyDescription("Análise crítica do perfil geral do candidato")
        String overallProfile,

        @JsonPropertyDescription("Avaliação da estrutura, seções e legibilidade por robôs ATS")
        String structureEvaluation,

        @JsonPropertyDescription("Incoerências encontradas (ex: sobreposição de datas, gaps sem explicação, discrepâncias)")
        List<String> inconsistencies,

        @JsonPropertyDescription("Pontos fortes e destaques competitivos do currículo")
        List<String> strengths,

        @JsonPropertyDescription("Pontos fracos ou seções deficientes")
        List<String> weaknesses,

        @JsonPropertyDescription("Sugestões práticas e acionáveis de melhoria")
        List<String> improvementSuggestions,

        @JsonPropertyDescription("Nota geral de compatibilidade e qualidade do currículo frente ao objetivo (0 a 100)")
        int score,

        @JsonPropertyDescription("Resumo executivo do diagnóstico para o candidato")
        String atsCompatibilitySummary
) {
}
