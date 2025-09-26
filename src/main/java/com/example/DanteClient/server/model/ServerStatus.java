package com.example.DanteClient.server.model;

import java.time.LocalDateTime;

/**
 * Modelo para representar el estado de un servidor
 */
public class ServerStatus {
    
    private String serverAddress;
    private String port;
    private boolean isReachable;
    private long responseTimeMs;
    private String status;
    private LocalDateTime lastChecked;
    private String errorMessage;
    private int timeoutMs;
    
    public ServerStatus() {
        this.lastChecked = LocalDateTime.now();
    }
    
    public ServerStatus(String serverAddress, String port) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.lastChecked = LocalDateTime.now();
    }
    
    // Getters y Setters
    public String getServerAddress() {
        return serverAddress;
    }
    
    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
    
    public String getPort() {
        return port;
    }
    
    public void setPort(String port) {
        this.port = port;
    }
    
    public boolean isReachable() {
        return isReachable;
    }
    
    public void setReachable(boolean reachable) {
        isReachable = reachable;
        this.lastChecked = LocalDateTime.now();
    }
    
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getLastChecked() {
        return lastChecked;
    }
    
    public void setLastChecked(LocalDateTime lastChecked) {
        this.lastChecked = lastChecked;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public int getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    /**
     * Obtiene la URL completa del servidor
     */
    public String getFullUrl() {
        return "http://" + serverAddress + ":" + port;
    }
    
    /**
     * Obtiene descripción del estado de conexión
     */
    public String getConnectionDescription() {
        if (isReachable) {
            if (responseTimeMs < 100) return "Excelente";
            if (responseTimeMs < 500) return "Buena";
            if (responseTimeMs < 1000) return "Aceptable";
            return "Lenta";
        }
        return "Sin conexión";
    }
    
    /**
     * Verifica si el servidor responde rápidamente
     */
    public boolean isFastResponse() {
        return isReachable && responseTimeMs < 200;
    }
    
    @Override
    public String toString() {
        return "ServerStatus{" +
                "serverAddress='" + serverAddress + '\'' +
                ", port='" + port + '\'' +
                ", isReachable=" + isReachable +
                ", responseTimeMs=" + responseTimeMs +
                ", status='" + status + '\'' +
                ", lastChecked=" + lastChecked +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}