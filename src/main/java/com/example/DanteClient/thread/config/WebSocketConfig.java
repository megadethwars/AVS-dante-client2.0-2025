package com.example.DanteClient.thread.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Configuración de WebSocket para comunicación de volumen de canales
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private VolumeWebSocketHandler volumeWebSocketHandler;
    
    @Autowired
    private ThreadWebSocketHandler threadWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Registrar el handler para el endpoint de volumen
        registry.addHandler(volumeWebSocketHandler, "/ws/volume")
                .setAllowedOrigins("*"); // Permitir conexiones desde cualquier origen
        
        // Registrar el handler para el endpoint de notificaciones de threads
        registry.addHandler(threadWebSocketHandler, "/ws/thread")
                .setAllowedOrigins("*"); // Permitir conexiones desde cualquier origen
    }
}