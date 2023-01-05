package com.vibrent.drc.exception;

/**
 * BusinessProcessingException
 */
public class NonRecoverableException extends ApiRequestException {

    public NonRecoverableException(String message) {
        super(message);
    }

    public NonRecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
