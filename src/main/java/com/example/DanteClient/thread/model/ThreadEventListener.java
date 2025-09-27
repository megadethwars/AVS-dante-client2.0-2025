package com.example.DanteClient.thread.model;

/**
 * Interfaz para notificaciones de eventos del ChannelThread
 */
public interface ThreadEventListener {
    
    /**
     * Se llama cuando el thread termina normalmente
     */
    void onThreadFinished(int channelId, String channelName, String reason);
    
    /**
     * Se llama cuando ocurre una excepción en el thread
     */
    void onThreadException(int channelId, String channelName, String exceptionType, String errorMessage);
    
    /**
     * Se llama cuando cambia el estado del thread
     */
    void onThreadStatusChanged(int channelId, String channelName, String oldStatus, String newStatus);
}