package com.atsoptimizer.modules.resume.dto;

import com.atsoptimizer.modules.resume.domain.Resume;
import com.atsoptimizer.modules.resume.domain.ResumeStatus;

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
