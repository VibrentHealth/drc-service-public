package com.vibrent.drc.exception;

/**
 * BusinessProcessingException
 */
public class BusinessProcessingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessProcessingException(String message) {
        super(message);
    }

    public BusinessProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
