package com.atsoptimizer.modules.jobmatch.service;

import com.atsoptimizer.modules.jobmatch.domain.JobRequirements;
import com.atsoptimizer.modules.jobmatch.domain.JobSkillRequirement;
import com.atsoptimizer.modules.jobmatch.domain.KeywordMatchReport;
import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.ResumeAnalysis;
import com.atsoptimizer.modules.resume.domain.SkillCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordMatcherTest {

    private KeywordMatcher keywordMatcher;

    @BeforeEach
    void setUp() {
        keywordMatcher = new KeywordMatcher();
    }

    @Test
    @DisplayName("Should match canonical skills and aliases accurately while avoiding substring false positives")
    void shouldMatchSkillsWithAliasesAndAvoidFalsePositives() {
        Resume resume = new Resume(
                1L,
                "cv.pdf",
                "application/pdf",
                1000L,
                "Desenvolvedor com experiência em JavaScript, C++, .NET, Postgres e desenvolvimento de código limpo com Spring."
        );

        ResumeAnalysis analysis = new ResumeAnalysis(
                1L, 85, "Lucas", "Dev", null, "Resumo com Spring Boot e Docker",
                List.of(), List.of(), List.of(), List.of(),
                List.of(new SkillCategory("Linguagens", List.of("JavaScript", "C++", "SQL"))),
                List.of(),
                "Objetivo", "Perfil", "Estrutura", "ATS",
                List.of("Docker", "Microsservicos"),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                LocalDateTime.now()
        );
        resume.completeAnalysis(analysis);

        JobRequirements requirements = new JobRequirements(
                "Engenheiro Backend",
                "Pleno",
                List.of(
                        new JobSkillRequirement("PostgreSQL", List.of("Postgres", "PGSQL")),
                        new JobSkillRequirement("C++", List.of()),
                        new JobSkillRequirement(".NET", List.of("ASP.NET")),
                        new JobSkillRequirement("Java", List.of()),
                        new JobSkillRequirement("Go", List.of("Golang"))
                ),
                List.of(
                        new JobSkillRequirement("Docker", List.of()),
                        new JobSkillRequirement("Kubernetes", List.of("K8s"))
                ),
                List.of("Comunicação"),
                List.of(),
                List.of()
        );

        KeywordMatchReport report = keywordMatcher.evaluate(resume, requirements);

        assertThat(report.matchedRequiredSkills()).containsExactlyInAnyOrder("PostgreSQL", "C++", ".NET");
        assertThat(report.missingRequiredSkills()).containsExactlyInAnyOrder("Java", "Go");
        assertThat(report.requiredSkillsMatchPercentage()).isEqualTo(60);

        assertThat(report.matchedPreferredSkills()).containsExactly("Docker");
        assertThat(report.missingPreferredSkills()).containsExactly("Kubernetes");
        assertThat(report.preferredSkillsMatchPercentage()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should normalize accents and match case-insensitively")
    void shouldNormalizeAccentsAndCase() {
        Resume resume = new Resume(
                2L,
                "cv.pdf",
                "application/pdf",
                500L,
                "Conhecimento sólido em Orquestração de Contêineres, Banco de Dados Relacional e APIs RESTful."
        );

        JobRequirements requirements = new JobRequirements(
                "Backend",
                "Júnior",
                List.of(
                        new JobSkillRequirement("Orquestracao de Conteineres", List.of()),
                        new JobSkillRequirement("APIs RESTful", List.of("REST"))
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        KeywordMatchReport report = keywordMatcher.evaluate(resume, requirements);

        assertThat(report.requiredSkillsMatchPercentage()).isEqualTo(100);
        assertThat(report.preferredSkillsMatchPercentage()).isEqualTo(100);
        assertThat(report.missingRequiredSkills()).isEmpty();
    }
}
