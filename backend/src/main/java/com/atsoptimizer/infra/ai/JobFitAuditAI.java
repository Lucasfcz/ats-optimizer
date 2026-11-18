package com.atsoptimizer.infra.ai;

import com.atsoptimizer.infra.ai.dto.JobFitAuditAiResult;
import com.atsoptimizer.modules.jobmatch.domain.JobRequirements;
import com.atsoptimizer.modules.jobmatch.domain.KeywordMatchReport;
import com.atsoptimizer.modules.resume.domain.ResumeAnalysis;
import org.springframework.stereotype.Component;

/**
 * Axis 2 AI component: evaluates semantic fit, seniority alignment, impact metrics,
 * and concrete improvement actions from a Tech Recruiter & ATS perspective.
 */
@Component
public class JobFitAuditAI {

    private final AiClient aiClient;
    private final PromptContextSerializer serializer;

    public JobFitAuditAI(AiClient aiClient, PromptContextSerializer serializer) {
        this.aiClient = aiClient;
        this.serializer = serializer;
    }

    public JobFitAuditAiResult evaluateFit(
            ResumeAnalysis candidateAnalysis,
            String rawResumeText,
            JobRequirements jobRequirements,
            KeywordMatchReport keywordReport,
            String rawJobDescription
    ) {
        String systemPrompt = """
                Você é um Tech Recruiter Sênior e Especialista em Sistemas ATS avaliando a aderência de um candidato para UMA vaga específica.
                O foco desta auditoria é 100% maximizar a aprovação na triagem automática (ATS) e técnica do recrutador dessa vaga.

                Você receberá:
                1. O perfil estruturado e texto bruto do candidato.
                2. Os requisitos estruturados e a descrição da vaga.
                3. O relatório determinístico de palavras-chave (skills já encontradas vs ausentes calculadas pelo código).

                Sua resposta JSON deve conter:
                - semanticFitScore (0 a 100): aderência qualitativa de senioridade, profundidade de experiência, domínio e responsabilidades da vaga.
                - structureImpactScore (0 a 100): clareza da narrativa, métricas quantificáveis, verbos de ação e força do currículo frente à vaga.
                - executiveSummary: diagnóstico direto explicando o nível de compatibilidade com a vaga.
                - matchReasons: motivos concretos e específicos pelos quais o currículo ganhou ou perdeu pontos nesta vaga (cite tecnologias e experiências reais).
                - criticalGaps: lacunas técnicas ou de contexto que mais prejudicam a aprovação no ATS desta vaga.
                - improvementRecommendations: ações práticas e específicas para otimizar este currículo para esta vaga.

                Seja rigoroso, técnico e fiel aos fatos do currículo. Não contradiga o relatório determinístico de keywords.
                """;

        String userPrompt = """
                --- RELATÓRIO DETERMINÍSTICO DE KEYWORDS ---
                %s

                --- REQUISITOS DA VAGA ---
                %s

                --- DESCRIÇÃO COMPLETA DA VAGA ---
                %s

                --- DADOS ESTRUTURADOS DO CANDIDATO ---
                %s

                --- TEXTO BRUTO DO CURRÍCULO ---
                %s
                """.formatted(
                serializer.toJson(keywordReport, "keywordReport"),
                serializer.toJson(jobRequirements, "jobRequirements"),
                rawJobDescription,
                serializer.toJson(candidateAnalysis, "candidateAnalysis"),
                rawResumeText
        );

        return aiClient.sendPrompt(systemPrompt, userPrompt, JobFitAuditAiResult.class);
    }
}
