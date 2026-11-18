package com.atsoptimizer.infra.ai;

import com.atsoptimizer.infra.ai.dto.StructuredResumeAiResult;
import com.atsoptimizer.modules.jobmatch.domain.JobFitAudit;
import com.atsoptimizer.modules.jobmatch.domain.JobRequirements;
import com.atsoptimizer.modules.jobmatch.domain.KeywordMatchReport;
import com.atsoptimizer.modules.jobmatch.domain.StructuredResumeTemplateData;
import com.atsoptimizer.modules.resume.domain.ResumeAnalysis;
import org.springframework.stereotype.Component;

/**
 * Axis 3B AI component (Ideal Profile Mode): builds an ATS-ideal resume anchored on the
 * candidate's base identity while incorporating missing job requirements and listing all injected gaps.
 */
@Component
public class IdealResumeFormatterAI {

    private final AiClient aiClient;
    private final PromptContextSerializer serializer;

    public IdealResumeFormatterAI(AiClient aiClient, PromptContextSerializer serializer) {
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
                Você é um especialista em otimização de currículos para ATS operando no MODO PERFIL PERFEITO (100%% ADERÊNCIA À VAGA).

                %s

                REGRAS DO MODO PERFIL PERFEITO:
                1. Preserve a identidade básica e o histórico real do candidato (nome, contatos, localização, formação acadêmica e instituições/projetos existentes).
                2. Incorpore de forma natural, coesa e técnica as skills obrigatórias e desejáveis ausentes (missingRequiredSkills / missingPreferredSkills) tanto em Habilidades Técnicas quanto contextualizadas nos bullets e resumo para atingir ~100%% de aprovação no ATS da vaga.
                3. Adapte targetRoleTitle e o summary exatamente ao perfil buscado pela vaga.
                4. OBRIGATÓRIO: Liste explicitamente no campo injectedGaps cada skill, responsabilidade ou ajuste adicionado que NÃO estava presente no currículo original do candidato, para total transparência com o usuário.
                """.formatted(PromptContextSerializer.BASE_PDF_TEMPLATE_CONTRACT);

        String userPrompt = """
                Gere os textos estruturados no MODO PERFIL PERFEITO para a vaga abaixo:

                --- VAGA E REQUISITOS ---
                %s

                --- RELATÓRIO DE KEYWORDS (INCLUINDO GAPS A INCORPORAR) ---
                %s

                --- AUDITORIA E SUGESTÕES ---
                %s

                --- PERFIL BASE DO CANDIDATO ---
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
                Você está operando no MODO PERFIL PERFEITO:
                1. Sintetize o summary e os bullets reduzindo aproximadamente 20% do volume total de texto para caber confortavelmente em 1 página.
                2. Mantenha todas as palavras-chave técnicas críticas para passar no ATS da vaga.
                3. Preserve intactos o campo injectedGaps, suggestedFileName, contatos, formação e idiomas.
                """;

        String userPrompt = """
                Condense o seguinte JSON de currículo mantendo todas as seções no mesmo schema:

                %s
                """.formatted(serializer.toJson(oversizedData, "oversizedIdealData"));

        return aiClient.sendPrompt(systemPrompt, userPrompt, StructuredResumeAiResult.class);
    }
}
