package com.atsoptimizer.modules.jobmatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateJobMatchRequest(
        @NotNull(message = "Resume ID is required")
        Long resumeId,

        @NotBlank(message = "Job description cannot be empty")
        String jobDescription
) {
}
