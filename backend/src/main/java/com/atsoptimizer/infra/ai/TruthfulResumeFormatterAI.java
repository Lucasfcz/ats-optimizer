package com.atsoptimizer.infra.ai;

import com.atsoptimizer.infra.ai.dto.StructuredResumeAiResult;
import com.atsoptimizer.modules.jobmatch.domain.JobFitAudit;
import com.atsoptimizer.modules.jobmatch.domain.JobRequirements;
import com.atsoptimizer.modules.jobmatch.domain.KeywordMatchReport;
import com.atsoptimizer.modules.jobmatch.domain.StructuredResumeTemplateData;
import com.atsoptimizer.modules.resume.domain.ResumeAnalysis;
import org.springframework.stereotype.Component;

/**
 * Axis 3A AI component (Truthful Mode): delivers tailored structured resume text
 * matching the 1-page PDF template using strictly verified candidate facts.
 */
@Component
public class TruthfulResumeFormatterAI {

    private final AiClient aiClient;
    private final PromptContextSerializer serializer;

    public TruthfulResumeFormatterAI(AiClient aiClient, PromptContextSerializer serializer) {
        this.aiClient = aiClient;
        this.serializer = serializer;
    }

    public StructuredResumeAiResult generate(
            ResumeAnalysis candidateAnalysis,
            String rawResumeText,
            JobRequirements jobRequirements,
            KeywordMatchReport keywordReport,
            JobFitAudit fitAudit
    ) {
        String systemPrompt = """
                Você é um especialista em otimização de currículos para ATS operando no MODO FIEL (VERDADE ESTRITA).

                %s

                REGRAS INVIOLÁVEIS DO MODO FIEL:
                1. NUNCA invente, presuma ou adicione skills, empresas, cargos, datas, certificações ou métricas numéricas que não existam no currículo original do candidato.
                2. Otimize o vocabulário utilizando os termos exatos da vaga SOMENTE quando representarem experiências e conhecimentos reais presentes no perfil/texto bruto do candidato.
                3. Reescreva o headline (targetRoleTitle), o resumo e os bullets priorizando o que mais gera aderência ao ATS desta vaga sem mentir.
                4. O campo injectedGaps deve retornar lista vazia [].
                """.formatted(PromptContextSerializer.BASE_PDF_TEMPLATE_CONTRACT);

        String userPrompt = """
                Gere os textos estruturados no MODO FIEL para a vaga abaixo:

                --- VAGA E REQUISITOS ---
                %s

                --- RELATÓRIO DE KEYWORDS ---
                %s

                --- AUDITORIA E SUGESTÕES ---
                %s

                --- PERFIL DO CANDIDATO ---
                %s

                --- TEXTO BRUTO DO CURRÍCULO ORIGINAL ---
                %s
                """.formatted(
                serializer.toJson(jobRequirements, "jobRequirements"),
                serializer.toJson(keywordReport, "keywordReport"),
                serializer.toJson(fitAudit, "fitAudit"),
                serializer.toJson(candidateAnalysis, "candidateAnalysis"),
                rawResumeText
        );

        return aiClient.sendPrompt(systemPrompt, userPrompt, StructuredResumeAiResult.class);
    }

    public StructuredResumeAiResult condenseToSinglePage(StructuredResumeTemplateData oversizedData) {
        String systemPrompt = """
                O conteúdo estruturado abaixo ultrapassou 1 página A4 no renderizador de PDF.
                Você está operando no MODO FIEL (VERDADE ESTRITA):
                1. Sintetize o summary e os bullets reduzindo aproximadamente 20% do volume total de texto para caber confortavelmente em 1 página.
                2. Remova redundâncias verbais mantendo TODAS as palavras-chave técnicas importantes e preservando 100% dos fatos reais (NUNCA invente dados novos).
                3. Mantenha o campo injectedGaps como lista vazia [] e preserve suggestedFileName, contatos, formação e idiomas.
                """;

        String userPrompt = """
                Condense o seguinte JSON de currículo mantendo todas as seções no mesmo schema:

                %s
                """.formatted(serializer.toJson(oversizedData, "oversizedTruthfulData"));

        return aiClient.sendPrompt(systemPrompt, userPrompt, StructuredResumeAiResult.class);
    }
}
