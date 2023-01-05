package com.vibrent.drc.exception;

/**
 * BusinessProcessingException
 */
public class RecoverableException extends ApiRequestException {

    public RecoverableException(String message) {
        super(message);
    }

    public RecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
