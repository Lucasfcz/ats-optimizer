package com.atsoptimizer.infra.ai;

import com.atsoptimizer.infra.ai.dto.JobRequirementsAiResult;
import org.springframework.stereotype.Component;

/**
 * Axis 1 AI component: parses raw job descriptions into normalized requirements
 * and known technical aliases for deterministic keyword matching.
 */
@Component
public class JobDescriptionParserAI {

    private final AiClient aiClient;

    public JobDescriptionParserAI(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public JobRequirementsAiResult extractRequirements(String jobDescription) {
        String systemPrompt = """
                Você é um parser técnico de vagas e especialista em sistemas ATS (Greenhouse, Lever, Workday, Gupy).
                Sua tarefa é extrair com precisão todos os requisitos da descrição da vaga fornecida.

                Diretrizes obrigatórias:
                1. jobTitle: título principal do cargo da vaga.
                2. seniorityLevel: nível esperado (ex: Estágio, Júnior, Pleno, Sênior, Especialista ou Não especificado).
                3. requiredHardSkills: tecnologias, linguagens, frameworks, bancos de dados e ferramentas OBRIGATÓRIAS.
                   - Para cada skill, informe canonicalName (nome principal limpo, ex: "PostgreSQL", "Spring Boot") e aliases (sinônimos/abreviações comuns que podem aparecer em currículos, ex: ["Postgres", "PGSQL"] para PostgreSQL, ["K8s"] para Kubernetes, ["JS"] para JavaScript).
                4. preferredHardSkills: tecnologias e diferenciais DESEJÁVEIS (com canonicalName e aliases).
                5. softSkills: competências comportamentais citadas.
                6. responsibilities: principais atribuições do cargo em frases objetivas.
                7. domainKeywords: conceitos de engenharia/negócio importantes (ex: "Microsserviços", "APIs RESTful", "SOLID", "CI/CD").

                Não invente requisitos que não constam na descrição da vaga. Retorne JSON estrito no schema solicitado.
                """;

        String userPrompt = """
                Extraia os requisitos estruturados da seguinte descrição de vaga:

                --- DESCRIÇÃO DA VAGA ---
                %s
                --- FIM DA DESCRIÇÃO ---
                """.formatted(jobDescription);

        return aiClient.sendPrompt(systemPrompt, userPrompt, JobRequirementsAiResult.class);
    }
}
