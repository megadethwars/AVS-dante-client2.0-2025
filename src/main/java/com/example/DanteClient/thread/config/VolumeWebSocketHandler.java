package com.example.DanteClient.thread.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Handler de WebSocket para gestionar conexiones de volumen de canales
 */
@Component
public class VolumeWebSocketHandler extends TextWebSocketHandler {
    
    // Set de sesiones activas para broadcast
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        
        System.out.println("========= NEW WEBSOCKET CONNECTION =========");
        System.out.println("🔗 Session ID: " + session.getId());
        System.out.println("📍 Remote Address: " + session.getRemoteAddress());
        System.out.println("🌐 URI: " + session.getUri());
        System.out.println("🔧 Protocol: " + session.getAcceptedProtocol());
        System.out.println("📊 Total conexiones activas: " + sessions.size());
        System.out.println("⏰ Conectado en: " + java.time.LocalDateTime.now());
        System.out.println("=============================================");
        
        // Enviar mensaje de bienvenida con más detalles
        String welcomeMessage = String.format(
            "{\"type\":\"connection\",\"status\":\"connected\",\"sessionId\":\"%s\",\"totalConnections\":%d,\"timestamp\":\"%s\"}",
            session.getId(),
            sessions.size(),
            java.time.LocalDateTime.now().toString()
        );
        
        sendMessage(session, welcomeMessage);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        
        System.out.println("======== WEBSOCKET DISCONNECTION ========");
        System.out.println("❌ Session ID cerrada: " + session.getId());
        System.out.println("📍 Remote Address: " + session.getRemoteAddress());
        System.out.println("� Close Status: " + status.getCode() + " - " + status.getReason());
        System.out.println("📊 Conexiones restantes: " + sessions.size());
        System.out.println("⏰ Desconectado en: " + java.time.LocalDateTime.now());
        System.out.println("==========================================");
    }
    
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        System.out.println("====== WEBSOCKET MESSAGE RECEIVED ======");
        System.out.println("🔗 Session ID: " + session.getId());
        System.out.println("📍 Remote Address: " + session.getRemoteAddress());
        System.out.println("⏰ Timestamp: " + java.time.LocalDateTime.now());
        System.out.println("📦 Payload Length: " + payload.length() + " characters");
        System.out.println("📨 Raw Message: " + payload);
        
        // Intentar parsear como JSON para mostrar más detalles
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Object json = mapper.readValue(payload, Object.class);
            System.out.println("📋 Parsed JSON: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
        } catch (Exception e) {
            System.out.println("⚠️  No es JSON válido: " + e.getMessage());
        }
        
        System.out.println("==========================================");
        
        // Aquí se pueden manejar mensajes entrantes del cliente si es necesario
        // Por ahora solo enviamos confirmación
        String ackMessage = String.format(
            "{\"type\":\"ack\",\"message\":\"Mensaje recibido\",\"sessionId\":\"%s\",\"timestamp\":\"%s\"}",
            session.getId(),
            java.time.LocalDateTime.now().toString()
        );
        
        sendMessage(session, ackMessage);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("❌ Error en WebSocket " + session.getId() + ": " + exception.getMessage());
        if (session.isOpen()) {
            session.close();
        }
        sessions.remove(session);
    }
    
    /**
     * Envía un mensaje JSON de volumen a todas las conexiones activas
     */
    public void broadcastVolumeUpdate(int channelId, int volumeLevel) {
        String message = String.format(
            "{\"type\":\"volume\",\"channelId\":%d,\"volumeLevel\":%d,\"timestamp\":\"%s\"}",
            channelId, 
            volumeLevel, 
            java.time.LocalDateTime.now().toString()
        );
        
        System.out.println("======== BROADCASTING VOLUME UPDATE ========");
        System.out.println("🎚️ Canal ID: " + channelId);
        System.out.println("🔊 Nivel de volumen: " + volumeLevel);
        System.out.println("📡 Mensaje JSON: " + message);
        System.out.println("👥 Enviando a " + sessions.size() + " conexiones");
        System.out.println("=============================================");
        
        broadcastMessage(message);
    }
    
    /**
     * Envía un mensaje a una sesión específica
     */
    public void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            System.err.println("Error al enviar mensaje a sesión " + session.getId() + ": " + e.getMessage());
            sessions.remove(session);
        }
    }
    
    /**
     * Envía un mensaje a todas las conexiones activas
     */
    public void broadcastMessage(String message) {
        System.out.println("📡 Broadcasting mensaje: " + message);
        
        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                    return false; // Mantener sesión
                } else {
                    return true; // Remover sesión cerrada
                }
            } catch (IOException e) {
                System.err.println("Error al enviar broadcast a sesión " + session.getId() + ": " + e.getMessage());
                return true; // Remover sesión con error
            }
        });
        
        System.out.println("📊 Mensaje enviado a " + sessions.size() + " conexiones");
    }
    
    /**
     * Obtiene el número de conexiones activas
     */
    public int getActiveConnectionsCount() {
        return sessions.size();
    }
    
    /**
     * Envía estadísticas de conexiones
     */
    public void broadcastConnectionStats() {
        String statsMessage = String.format(
            "{\"type\":\"stats\",\"activeConnections\":%d,\"timestamp\":\"%s\"}",
            sessions.size(),
            java.time.LocalDateTime.now().toString()
        );
        
        broadcastMessage(statsMessage);
    }
}