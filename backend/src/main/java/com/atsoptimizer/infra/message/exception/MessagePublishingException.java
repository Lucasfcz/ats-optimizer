package com.atsoptimizer.infra.message.exception;

public class MessagePublishingException extends RuntimeException {

    public MessagePublishingException(String message) {
        super(message);
    }

    public MessagePublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
