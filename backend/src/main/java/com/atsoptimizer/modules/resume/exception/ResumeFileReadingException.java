package com.atsoptimizer.modules.resume.exception;

public class ResumeFileReadingException extends RuntimeException {

    public ResumeFileReadingException(String fileName, Throwable cause) {
        super("Could not read uploaded file content: " + fileName, cause);
    }

    public ResumeFileReadingException(String message) {
        super(message);
    }
}
