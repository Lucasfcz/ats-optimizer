package com.atsoptimizer.resume.dto;

import com.atsoptimizer.resume.domain.Resume;
import com.atsoptimizer.resume.domain.ResumeStatus;

import java.time.LocalDateTime;

public record ResumeResponseDto(
        Long id,
        String fileName,
        String contentType,
        Long fileSize,
        ResumeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ResumeResponseDto fromEntity(Resume resume) {
        return new ResumeResponseDto(
                resume.getId(),
                resume.getFileName(),
                resume.getContentType(),
                resume.getFileSize(),
                resume.getStatus(),
                resume.getCreatedAt(),
                resume.getUpdatedAt()
        );
    }
}
