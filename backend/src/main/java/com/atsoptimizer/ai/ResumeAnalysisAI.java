package com.atsoptimizer.ai;

import com.atsoptimizer.ai.dto.ResumeAnalysisResult;
import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisAI {

    private final AiClient aiClient;

    public ResumeAnalysisAI(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public ResumeAnalysisResult sendToAiAnalisysResume(String resumeText) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(resumeText);
        return aiClient.sendPrompt(systemPrompt, userPrompt, ResumeAnalysisResult.class);
    }

    private String buildSystemPrompt() {
        return """
                Você é um especialista sênior em Recrutamento & Seleção (Tech Recruiter) e Engenheiro de Sistemas ATS (Applicant Tracking Systems como Taleo, Greenhouse, Workday, Lever e Gupy).
                
                Sua missão é realizar uma auditoria rigorosa, minuciosa e técnica no currículo fornecido, avaliando:
                1. Palavras-chave: Termos técnicos, ferramentas, frameworks e metodologias presentes vs termos essenciais ausentes.
                2. Perfil Geral: Senioridade percebida, clareza na proposta de valor profissional e coerência da trajetória.
                3. Objetivo Profissional Inferido: Qual é o foco de carreira que o currículo transparece, mesmo se não estiver explicitado.
                4. Estrutura e Formatação ATS: Seções padronizadas, facilidade de leitura por parsers automatizados, hierarquia da informação.
                5. Incoerências: Lacunas temporais inexplicadas, regressão de cargos sem contexto, contradições entre habilidades e projetos.
                6. Pontos Fortes: O que torna o candidato competitivo no mercado.
                7. Pontos Fracos: Gaps críticos de informação, métricas ausentes (falta de impacto quantificável).
                8. Como Melhorar: Recomendações práticas, diretas e acionáveis para otimizar o currículo e passar nas triagens dos robôs ATS.
                9. Nota Geral (0 a 100%): Porcentagem calculada com base na solidez do currículo perante seu objetivo profissional e aderência aos padrões ATS.
                
                Forneça a resposta em formato JSON estrito aderente ao schema solicitado.
                """;
    }

    private String buildUserPrompt(String resumeText) {
        return """
                Analise minuciosamente o seguinte currículo extraído de forma bruta:
                
                --- INÍCIO DO CURRÍCULO ---
                %s
                --- FIM DO CURRÍCULO ---
                
                Avalie todas as seções e forneça o diagnóstico estruturado completo.
                """.formatted(resumeText);
    }
}
