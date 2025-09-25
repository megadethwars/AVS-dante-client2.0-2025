package com.example.DanteClient.thread.exception;

/**
 * Excepción base para errores relacionados con threads de canales
 */
public class ThreadException extends RuntimeException {
    
    private final int errorCode;
    private final String errorType;
    
    public ThreadException(int errorCode, String errorType, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = errorType;
    }
    
    public ThreadException(int errorCode, String errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorType = errorType;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public String getErrorType() {
        return errorType;
    }
}