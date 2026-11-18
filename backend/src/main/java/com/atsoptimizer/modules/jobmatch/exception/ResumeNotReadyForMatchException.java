package com.atsoptimizer.modules.jobmatch.exception;

import com.atsoptimizer.modules.resume.domain.ResumeStatus;

public class ResumeNotReadyForMatchException extends RuntimeException {

    public ResumeNotReadyForMatchException(Long resumeId, ResumeStatus currentStatus) {
        super("Resume [" + resumeId + "] is not ready for job matching. Current status: " + currentStatus);
    }
}
