package com.atsoptimizer.modules.jobmatch.exception;

public class EmptyJobDescriptionException extends RuntimeException {

    public EmptyJobDescriptionException() {
        super("Job description cannot be empty or blank");
    }
}
