package com.atsoptimizer.modules.jobmatch.domain;

public enum JobMatchStatus {
    PENDING_ANALYSIS,
    PARSING_JOB,
    AUDITING_FIT,
    ANALYZED,
    GENERATING_RESUME,
    COMPLETED,
    FAILED
}
