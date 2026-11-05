package com.atsoptimizer.infra.message.dto;

import java.io.Serializable;

public record ResumeAnalysisMessage(
        Long resumeId,
        String fileName,
        String extractedText
) implements Serializable {
}
