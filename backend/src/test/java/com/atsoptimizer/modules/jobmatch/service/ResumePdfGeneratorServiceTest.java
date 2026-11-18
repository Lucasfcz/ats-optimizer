package com.atsoptimizer.modules.jobmatch.service;

import com.atsoptimizer.modules.jobmatch.domain.StructuredResumeTemplateData;
import com.atsoptimizer.modules.jobmatch.domain.TemplateExperienceEntry;
import com.atsoptimizer.modules.jobmatch.exception.PdfGenerationException;
import com.atsoptimizer.modules.resume.domain.EducationItem;
import com.atsoptimizer.modules.resume.domain.LanguageItem;
import com.atsoptimizer.modules.resume.domain.SkillCategory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumePdfGeneratorServiceTest {

    private ResumePdfGeneratorService pdfGeneratorService;

    @BeforeEach
    void setUp() {
        pdfGeneratorService = new ResumePdfGeneratorService();
    }

    @Test
    @DisplayName("Should generate exact 1-page ATS PDF following official template structure and escaping XML chars")
    void shouldGenerateSinglePagePdfFollowingTemplate() throws IOException {
        StructuredResumeTemplateData data = buildSampleData(4);

        byte[] pdfBytes = pdfGeneratorService.generatePdf(data);

        assertThat(pdfBytes).isNotEmpty();
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Lucas Figueiredo Cabral");
            assertThat(text).contains("DESENVOLVEDOR BACK-END JAVA");
            assertThat(text).contains("HABILIDADES TÉCNICAS");
            assertThat(text).contains("C++");
            assertThat(text).contains("R&D");
            assertThat(text).contains("EXPERIÊNCIA E PROJETOS");
            assertThat(text).contains("Stack: Java 21, Spring Boot, PostgreSQL");
            assertThat(text).contains("FORMAÇÃO ACADÊMICA");
            assertThat(text).contains("CONQUISTAS");
            assertThat(text).contains("IDIOMAS");
        }
    }

    @Test
    @DisplayName("Should preserve 1-page limit even when content exceeds tightest layout profile via tail pruning")
    void shouldEnforceSinglePageEvenWithBloatedInput() throws IOException {
        StructuredResumeTemplateData bloatedData = buildSampleData(12);

        StructuredResumeTemplateData pruned = pdfGeneratorService.pruneToSinglePageIfNeeded(bloatedData);
        byte[] pdfBytes = pdfGeneratorService.generatePdf(pruned);

        assertThat(pruned.experiencesAndProjects().size()).isLessThan(12);
        assertThat(pdfGeneratorService.countPages(pdfBytes)).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw PdfGenerationException when resume data is null")
    void shouldThrowExceptionWhenDataIsNull() {
        assertThatThrownBy(() -> pdfGeneratorService.generatePdf(null))
                .isInstanceOf(PdfGenerationException.class);
    }

    private StructuredResumeTemplateData buildSampleData(int experienceCount) {
        List<TemplateExperienceEntry> entries = new ArrayList<>();
        for (int i = 1; i <= experienceCount; i++) {
            entries.add(new TemplateExperienceEntry(
                    "Projeto Backend Escalável " + i,
                    "2026",
                    i == 1 ? "Desenvolvedor Backend Java" : "Java 21, Spring Boot, PostgreSQL, Redis, Docker",
                    i != 1,
                    List.of(
                            "Desenvolvimento de APIs RESTful com Java 21, Spring Boot e práticas R&D.",
                            "Otimização de consultas SQL no PostgreSQL e cache distribuído com Redis.",
                            "Implementação de testes unitários com JUnit 5, Mockito e cobertura > 90%."
                    )
            ));
        }

        return new StructuredResumeTemplateData(
                "Lucas_Figueiredo_Cabral_Backend_Java.pdf",
                "Lucas Figueiredo Cabral",
                "Recife / Paulista, PE",
                "(81) 99358-2008",
                "lucasfc080@gmail.com",
                "linkedin.com/in/lucasfcz-dev",
                List.of("github.com/Lucasfcz"),
                "DESENVOLVEDOR BACK-END JAVA (ESTÁGIO / JÚNIOR)",
                "Estudante de Análise e Desenvolvimento de Sistemas no IFPE com experiência sólida em Java 21, Spring Boot, PostgreSQL, Docker e integração com APIs de IA.",
                List.of(
                        new SkillCategory("Linguagens", List.of("Java (17 e 21)", "SQL", "C++", "Python")),
                        new SkillCategory("Back-end e Arquitetura", List.of("Spring Boot", "Spring Security", "Spring Data JPA", "Spring AI", "SOLID")),
                        new SkillCategory("Bancos de Dados e DevOps", List.of("PostgreSQL", "Redis", "Docker", "RabbitMQ", "JUnit 5"))
                ),
                entries,
                List.of(new EducationItem("IFPE Campus Paulista", "Tecnólogo", "Análise e Desenvolvimento de Sistemas", "2026 a Dez/2028", "Previsão")),
                List.of("Medalhista de Bronze na XII Olimpíada Pernambucana de Informática (OPEI 2026)."),
                List.of(new LanguageItem("Português", "Nativo"), new LanguageItem("Inglês", "Intermediário (B1)")),
                List.of()
        );
    }
}
