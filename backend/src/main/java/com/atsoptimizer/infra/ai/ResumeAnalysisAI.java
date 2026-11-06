package com.atsoptimizer.infra.ai;

import com.atsoptimizer.infra.ai.dto.ResumeAnalysisResult;
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
                
                Sua missão é dupla:
                1. EXTRAÇÃO ESTRUTURADA DE DADOS DO CANDIDATO PARA RECONSTRUÇÃO EM PDF (1 PÁGINA):
                   - Extraia com fidelidade: nome completo (candidateName), título/headline profissional, informações de contato (telefone, e-mail, localização/cidade-estado, linkedin, outros links como GitHub/portfólio), resumo sobre o profissional (about), experiências (empresas, estágios, monitorias, projetos acadêmicos de extensão ou projetos pessoais/open-source com título, organização, período, localização, descrição concisa e conquistas/highlights), formação acadêmica (education), certificações (certifications) e idiomas (languages).
                   - DIRETRIZ CRÍTICA DE GERAÇÃO: Os dados extraídos serão utilizados diretamente para compor um novo currículo em PDF profissional de exatamente 1 PÁGINA. Portanto, redija e estruture cada campo de maneira concisa, objetiva e de alto impacto, priorizando síntese e clareza.
                   - REGRA DE FIDELIDADE (ZERO ALUCINAÇÃO): NUNCA invente, suponha ou adicione informações ou experiências que não existam no texto original. Se um dado não estiver presente, retorne null ou lista vazia.
                   - REGRA DE PRESERVAÇÃO: NÃO corte informações reais relevantes; apenas sintetize de forma concisa e assertiva preservando a verdade factual e todas as experiências e dados fornecidos.
                
                2. AUDITORIA RIGOROSA DE COMPATIBILIDADE ATS:
                   - Palavras-chave: Termos técnicos, ferramentas, frameworks e metodologias presentes vs essenciais ausentes.
                   - Perfil Geral e Objetivo Inferido: Senioridade percebida, proposta de valor e foco de carreira transparecido.
                   - Avaliação Estrutural ATS: Seções padronizadas, hierarquia da informação e leitura por parsers automatizados.
                   - Incoerências, Pontos Fortes e Pontos Fracos: Análise crítica de métricas ausentes, lacunas temporais e vantagens competitivas.
                   - Sugestões de Melhoria: Recomendações práticas e acionáveis para otimizar o currículo.
                   - Nota Geral (score de 0 a 100): Porcentagem de aderência e competitividade perante os padrões ATS e o mercado.
                
                Forneça a resposta em formato JSON estrito aderente ao schema solicitado.
                """;
    }

    private String buildUserPrompt(String resumeText) {
        return """
                Analise minuciosamente o seguinte currículo extraído de forma bruta:
                
                --- INÍCIO DO CURRÍCULO ---
                %s
                --- FIM DO CURRÍCULO ---
                
                Extraia todos os dados estruturados do candidato (otimizados para criação de novo PDF de 1 página, sem omitir dados reais e sem inventar nada) e forneça a auditoria ATS completa.
                """.formatted(resumeText);
    }
}
