package com.atsoptimizer.infra.extraction;

import com.atsoptimizer.infra.extraction.exception.DocumentExtractionException;
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
    @DisplayName("Should extract text and normalize whitespaces from a plain text stream")
    void shouldExtractTextAndNormalizeWhitespace() {
        String rawContent = "Lucas   Ferreira\n\n\nSoftware Engineer\t\tJava";
        InputStream stream = new ByteArrayInputStream(rawContent.getBytes(StandardCharsets.UTF_8));

        String extracted = extractor.extractText(stream, "curriculo.txt");

        assertThat(extracted).isEqualTo("Lucas Ferreira\n\nSoftware Engineer Java");
    }

    @Test
    @DisplayName("Should throw DocumentExtractionException when stream contains only whitespaces or is empty")
    void shouldThrowExceptionWhenContentIsEmpty() {
        InputStream emptyStream = new ByteArrayInputStream("   \n\n  \t ".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> extractor.extractText(emptyStream, "vazio.txt"))
                .isInstanceOf(DocumentExtractionException.class)
                .hasMessageContaining("Extracted content is empty for file: vazio.txt");
    }
}
