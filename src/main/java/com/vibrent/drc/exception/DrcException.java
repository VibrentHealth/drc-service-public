package com.vibrent.drc.exception;

/**
 * BusinessProcessingException
 */
public class DrcException extends Exception {

    private static final long serialVersionUID = 925644777481057003L;

    public DrcException(String message) {
        super(message);
    }

    public DrcException(String message, Throwable cause) {
        super(message, cause);
    }
}
