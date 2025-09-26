package com.example.DanteClient.data.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Clase para respuestas exitosas estructuradas en JSON
 */
public class SuccessResponse {
    private boolean success;
    private String message;
    private Object data;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    public SuccessResponse() {
        this.success = true;
        this.timestamp = LocalDateTime.now();
    }
    
    public SuccessResponse(String message) {
        this();
        this.message = message;
    }
    
    public SuccessResponse(String message, Object data) {
        this();
        this.message = message;
        this.data = data;
    }
    
    // Getters y setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}