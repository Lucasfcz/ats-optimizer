package com.atsoptimizer.modules.resume.exception;

public class ResumeNotFoundException extends RuntimeException {

    public ResumeNotFoundException(Long id) {
        super("Resume not found with id: " + id);
    }

    public ResumeNotFoundException(String message) {
        super(message);
    }
}
