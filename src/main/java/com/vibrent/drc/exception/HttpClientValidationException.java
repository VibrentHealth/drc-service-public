package com.vibrent.drc.exception;

/**
 * HttpClientValidationException
 */
public class HttpClientValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final int statusCode;

    public HttpClientValidationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
