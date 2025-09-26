package com.example.DanteClient.data.exception;

/**
 * Excepción base para errores relacionados con la configuración de Dante
 */
public class ConfigException extends RuntimeException {
    
    private final int errorCode;
    private final String errorType;
    
    public ConfigException(int errorCode, String errorType, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = errorType;
    }
    
    public ConfigException(int errorCode, String errorType, String message, Throwable cause) {
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