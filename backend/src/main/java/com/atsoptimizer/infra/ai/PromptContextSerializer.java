package com.atsoptimizer.infra.ai;

import com.atsoptimizer.infra.ai.exception.AiProcessingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Helper component to serialize domain snapshots into JSON strings for AI prompt context.
 */
@Component
class PromptContextSerializer {

    static final String BASE_PDF_TEMPLATE_CONTRACT = """
            Você NÃO cria HTML nem PDF; sua função é entregar exclusivamente os TEXTOS ESTRUTURADOS no JSON solicitado para alimentar nosso algoritmo gerador de PDF de EXATAMENTE 1 PÁGINA A4.

            ORÇAMENTO ESPACIAL OBRIGATÓRIO PARA CABER EM 1 PÁGINA:
            - summary: 1 parágrafo único, direto e de alto impacto (entre 450 e 620 caracteres).
            - technicalSkills: agrupe em 3 a 4 categorias claras (ex: Linguagens, Back-end e Arquitetura, Bancos de Dados e Cache, Testes e DevOps).
            - experiencesAndProjects: ordene por MAIOR relevância para esta vaga (máximo 4 blocos; 2 a 4 bullets objetivos por bloco; use isStack=true quando o subtítulo listar a stack do projeto e isStack=false quando for cargo).
            - suggestedFileName: nome profissional do arquivo PDF sem espaços ou acentos (ex: "Nome_Sobrenome_Cargo_Vaga.pdf").
            - NÃO use tags HTML nem formatação Markdown (**negrito**, etc.) dentro dos campos de texto.
            """;

    private final ObjectMapper objectMapper;

    PromptContextSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String toJson(Object value, String label) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new AiProcessingException("Failed to serialize context object for prompt: " + label, e);
        }
    }
}
