package com.atsoptimizer.infra.extraction;

import java.io.InputStream;

public interface DocumentTextExtractor {

    /**
     * Extracts text content from a given document input stream.
     *
     * @param inputStream the stream of the document (PDF, DOCX, etc.)
     * @param fileName the original filename for logging or metadata detection
     * @return the extracted raw text sanitized of excessive blank characters
     */
    String extractText(InputStream inputStream, String fileName);
}
