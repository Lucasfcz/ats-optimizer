package com.atsoptimizer.parser;

import com.atsoptimizer.parser.exception.DocumentExtractionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TikaDocumentTextExtractorTest {

    private TikaDocumentTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new TikaDocumentTextExtractor();
    }

    @Test
    @DisplayName("Should successfully extract and normalize text from a document stream")
    void shouldExtractTextSuccessfully() {
        String originalContent = "Lucas Silva\n\nDesenvolvedor Java Sênior\n\nExperiência em Spring Boot e Microsserviços.";
        InputStream stream = new ByteArrayInputStream(originalContent.getBytes(StandardCharsets.UTF_8));

        String extracted = extractor.extractText(stream, "curriculo.txt");

        assertThat(extracted)
                .isNotBlank()
                .contains("Lucas Silva")
                .contains("Desenvolvedor Java Sênior")
                .contains("Spring Boot");
    }

    @Test
    @DisplayName("Should throw DocumentExtractionException when content is blank or empty")
    void shouldThrowExceptionWhenContentIsEmpty() {
        InputStream emptyStream = new ByteArrayInputStream("   \n\n   ".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> extractor.extractText(emptyStream, "vazio.txt"))
                .isInstanceOf(DocumentExtractionException.class)
                .hasMessageContaining("Extracted content is empty");
    }
}
