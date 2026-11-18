package com.atsoptimizer.modules.jobmatch.exception;

import com.atsoptimizer.modules.jobmatch.domain.JobMatchStatus;

public class JobMatchNotReadyForGenerationException extends RuntimeException {

    public JobMatchNotReadyForGenerationException(Long jobMatchId, JobMatchStatus currentStatus) {
        super("Job match [" + jobMatchId + "] must complete initial analysis before generating a resume. Current status: " + currentStatus);
    }
}
