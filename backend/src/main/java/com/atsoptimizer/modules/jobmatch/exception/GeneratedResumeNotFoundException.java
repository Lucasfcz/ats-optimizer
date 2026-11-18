package com.atsoptimizer.modules.jobmatch.exception;

import com.atsoptimizer.modules.jobmatch.domain.OptimizationMode;

public class GeneratedResumeNotFoundException extends RuntimeException {

    public GeneratedResumeNotFoundException(Long jobMatchId, OptimizationMode mode) {
        super("Generated resume data not found for job match [" + jobMatchId + "] and mode [" + mode + "]. Trigger generation first.");
    }
}
