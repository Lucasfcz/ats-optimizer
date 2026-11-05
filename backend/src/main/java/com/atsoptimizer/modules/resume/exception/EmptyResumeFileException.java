package com.atsoptimizer.modules.resume.exception;

public class EmptyResumeFileException extends RuntimeException {

    public EmptyResumeFileException() {
        super("Uploaded resume file cannot be empty");
    }

    public EmptyResumeFileException(String message) {
        super(message);
    }
}
