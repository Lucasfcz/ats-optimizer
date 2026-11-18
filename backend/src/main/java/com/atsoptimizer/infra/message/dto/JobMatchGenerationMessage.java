package com.atsoptimizer.infra.message.dto;

import com.atsoptimizer.modules.jobmatch.domain.OptimizationMode;

import java.io.Serializable;

public record JobMatchGenerationMessage(
        Long jobMatchId,
        OptimizationMode mode
) implements Serializable {
}
