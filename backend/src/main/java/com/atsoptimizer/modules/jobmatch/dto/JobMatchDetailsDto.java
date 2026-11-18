package com.atsoptimizer.modules.jobmatch.dto;

import com.atsoptimizer.modules.jobmatch.domain.JobFitAudit;
import com.atsoptimizer.modules.jobmatch.domain.JobMatch;
import com.atsoptimizer.modules.jobmatch.domain.JobMatchStatus;
import com.atsoptimizer.modules.jobmatch.domain.JobRequirements;
import com.atsoptimizer.modules.jobmatch.domain.KeywordMatchReport;
import com.atsoptimizer.modules.jobmatch.domain.StructuredResumeTemplateData;

import java.time.LocalDateTime;

public record JobMatchDetailsDto(
        Long id,
        Long resumeId,
        JobMatchStatus status,
        String errorMessage,
        String jobDescription,
        JobRequirements jobRequirements,
        KeywordMatchReport keywordMatchReport,
        JobFitAudit fitAudit,
        StructuredResumeTemplateData truthfulResumeData,
        StructuredResumeTemplateData idealResumeData,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static JobMatchDetailsDto fromEntity(JobMatch jobMatch) {
        return new JobMatchDetailsDto(
                jobMatch.getId(),
                jobMatch.getResume().getId(),
                jobMatch.getStatus(),
                jobMatch.getErrorMessage(),
                jobMatch.getJobDescription(),
                jobMatch.getJobRequirements(),
                jobMatch.getKeywordMatchReport(),
                jobMatch.getFitAudit(),
                jobMatch.getTruthfulResumeData(),
                jobMatch.getIdealResumeData(),
                jobMatch.getCreatedAt(),
                jobMatch.getUpdatedAt()
        );
    }
}
