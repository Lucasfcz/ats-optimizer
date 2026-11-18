package com.atsoptimizer.modules.jobmatch.dto;

import com.atsoptimizer.modules.jobmatch.domain.JobMatch;
import com.atsoptimizer.modules.jobmatch.domain.JobMatchStatus;

import java.time.LocalDateTime;

public record JobMatchResponseDto(
        Long id,
        Long resumeId,
        JobMatchStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static JobMatchResponseDto fromEntity(JobMatch jobMatch) {
        return new JobMatchResponseDto(
                jobMatch.getId(),
                jobMatch.getResume().getId(),
                jobMatch.getStatus(),
                jobMatch.getCreatedAt(),
                jobMatch.getUpdatedAt()
        );
    }
}
