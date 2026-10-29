package com.wtoffice.ai;

public class AiCallException extends RuntimeException {
    private final String errorCode;

    public AiCallException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AiCallException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
