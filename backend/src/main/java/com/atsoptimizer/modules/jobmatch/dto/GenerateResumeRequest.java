package com.atsoptimizer.modules.jobmatch.dto;

import com.atsoptimizer.modules.jobmatch.domain.OptimizationMode;
import jakarta.validation.constraints.NotNull;

public record GenerateResumeRequest(
        @NotNull(message = "Optimization mode (TRUTHFUL or IDEAL) is required")
        OptimizationMode mode
) {
}
