package com.atsoptimizer.modules.resume.exception;

public class ResumeDomainException extends RuntimeException {

    public ResumeDomainException(String message) {
        super(message);
    }

    public ResumeDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
