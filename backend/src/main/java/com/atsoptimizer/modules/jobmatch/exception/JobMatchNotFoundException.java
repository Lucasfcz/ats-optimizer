package com.atsoptimizer.modules.jobmatch.exception;

public class JobMatchNotFoundException extends RuntimeException {

    public JobMatchNotFoundException(Long id) {
        super("Job match not found with id: " + id);
    }
}
