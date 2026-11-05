package com.atsoptimizer.infra.extraction;

import com.atsoptimizer.infra.extraction.exception.DocumentExtractionException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class TikaDocumentTextExtractor implements DocumentTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(TikaDocumentTextExtractor.class);
    private final Tika tika;

    public TikaDocumentTextExtractor() {
        this.tika = new Tika();
    }

    @Override
    public String extractText(InputStream inputStream, String fileName) {
        log.info("Starting synchronous text extraction with Apache Tika for file: {}", fileName);
        try {
            String extracted = tika.parseToString(inputStream);
            if (extracted == null || extracted.isBlank()) {
                throw new DocumentExtractionException("Extracted content is empty for file: " + fileName);
            }
            // Normalize multiple whitespaces/newlines while keeping paragraphs readable
            String normalized = extracted
                    .replaceAll("\r\n", "\n")
                    .replaceAll("[ \t]+", " ")
                    .replaceAll("\n{3,}", "\n\n")
                    .trim();

            log.info("Successfully extracted {} characters from {}", normalized.length(), fileName);
            return normalized;
        } catch (IOException | TikaException e) {
            log.error("Failed to extract text from file: {}", fileName, e);
            throw new DocumentExtractionException("Failed to extract text from document: " + fileName, e);
        }
    }
}
