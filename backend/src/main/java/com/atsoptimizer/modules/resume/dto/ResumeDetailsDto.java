package com.atsoptimizer.modules.resume.dto;

import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.ResumeAnalysis;
import com.atsoptimizer.modules.resume.domain.ResumeStatus;

import java.time.LocalDateTime;

public record ResumeDetailsDto(
        Long id,
        String fileName,
        String contentType,
        Long fileSize,
        ResumeStatus status,
        String errorMessage,
        String extractedText,
        ResumeAnalysis analysis,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ResumeDetailsDto fromEntity(Resume resume) {
        return new ResumeDetailsDto(
                resume.getId(),
                resume.getFileName(),
                resume.getContentType(),
                resume.getFileSize(),
                resume.getStatus(),
                resume.getErrorMessage(),
                resume.getExtractedText(),
                resume.getAnalysis(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }
}
