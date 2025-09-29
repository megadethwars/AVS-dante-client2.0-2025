package com.example.DanteClient.thread.config;

import com.example.DanteClient.thread.service.ChannelThreadService;
import com.example.DanteClient.thread.model.ChannelThread;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Map;
import java.util.HashMap;

// Import ChannelVolumeManager if it exists in your project
import com.example.DanteClient.thread.service.ChannelVolumeManager;

/**
 * Handler de WebSocket para gestionar conexiones de volumen de canales
 */
@Component
public class VolumeWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private ChannelThreadService threadService;

    @Autowired
    private ChannelVolumeManager volumeManager;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
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
        
        try {
            // Parsear el mensaje JSON
            JsonNode jsonNode = objectMapper.readTree(payload);
            System.out.println("📋 Parsed JSON: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
            
            // Verificar si es un comando de volumen
            if (jsonNode.has("channelId") && jsonNode.has("volume")) {
                int channelId = jsonNode.get("channelId").asInt();
                int volume = jsonNode.get("volume").asInt();
                
                System.out.println("🎚️ Procesando comando de volumen - Canal: " + channelId + ", Volumen: " + volume);
                
                // Procesar el comando de volumen
                processVolumeCommand(session, channelId, volume);
            } else {
                // Mensaje no reconocido
                String errorMessage = createErrorResponse("INVALID_FORMAT", "Formato de mensaje no válido. Se esperaba: {\"channelId\":1,\"volume\":60}", session.getId());
                sendMessage(session, errorMessage);
            }
            
        } catch (Exception e) {
            System.out.println("⚠️  Error al procesar JSON: " + e.getMessage());
            
            // Enviar mensaje de error por JSON inválido
            String errorMessage = createErrorResponse("JSON_PARSE_ERROR", "Error al parsear JSON: " + e.getMessage(), session.getId());
            sendMessage(session, errorMessage);
        }
        
        System.out.println("==========================================");
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
     * Procesa un comando de volumen recibido por WebSocket
     */
    private void processVolumeCommand(WebSocketSession session, int channelId, int volume) {
        try {
            System.out.println("🎚️ ======== PROCESSING VOLUME COMMAND ========");
            System.out.println("🔢 Canal ID: " + channelId);
            System.out.println("🔊 Volumen solicitado: " + volume);
            
            // Validar rango de volumen (0-100)
            if (volume < 0 || volume > 100) {
                String errorMessage = createErrorResponse(
                    "INVALID_VOLUME_RANGE", 
                    "El volumen debe estar entre 0 y 100. Recibido: " + volume, 
                    session.getId()
                );
                sendMessage(session, errorMessage);
                return;
            }
            
            // Verificar si el thread del canal está activo
            boolean isThreadActive = threadService.isThreadActive(channelId);
            
            if (isThreadActive) {
                // Thread activo - enviar volumen al thread
                ChannelThread channelThread = threadService.getThreadInfo(channelId);
                
                if (channelThread != null) {
                    // Setear el volumen en el thread
                    channelThread.setVolume(volume);
                    System.out.println("✅ Volumen " + volume + " aplicado al thread del canal " + channelId);
                    
                    // Crear respuesta de éxito
                    Map<String, Object> successResponse = new HashMap<>();
                    successResponse.put("type", "volume_success");
                    successResponse.put("channelId", channelId);
                    successResponse.put("volume", volume);
                    successResponse.put("status", "applied");
                    successResponse.put("threadStatus", channelThread.getStatus());
                    successResponse.put("message", "Volumen aplicado correctamente al canal " + channelId);
                    successResponse.put("timestamp", java.time.LocalDateTime.now().toString());
                    
                    String successMessage = objectMapper.writeValueAsString(successResponse);
                    sendMessage(session, successMessage);
                    volumeManager.setVolume(channelId, volume);
                    // Broadcast del cambio de volumen a todas las conexiones
                    broadcastVolumeUpdate(channelId, volume);
                    
                } else {
                    String errorMessage = createErrorResponse(
                        "THREAD_INFO_ERROR", 
                        "No se pudo obtener información del thread del canal " + channelId, 
                        session.getId()
                    );
                    sendMessage(session, errorMessage);
                }
                
            } else {
                // Thread no activo - enviar mensaje de error
                System.out.println("⚠️ Canal " + channelId + " no tiene thread activo");
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("type", "volume_error");
                errorResponse.put("errorCode", "CHANNEL_NOT_ACTIVE");
                errorResponse.put("channelId", channelId);
                errorResponse.put("requestedVolume", volume);
                errorResponse.put("message", "El canal " + channelId + " no está activo. Inicia el thread del canal primero.");
                errorResponse.put("suggestion", "Usa POST /api/threads/channel/" + channelId + " para activar el canal");
                errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
                
                String errorMessage = objectMapper.writeValueAsString(errorResponse);
                sendMessage(session, errorMessage);
            }
            
            System.out.println("🎚️ ===========================================");
            
        } catch (Exception e) {
            System.err.println("❌ Error procesando comando de volumen: " + e.getMessage());
            e.printStackTrace();
            
            String errorMessage = createErrorResponse(
                "PROCESSING_ERROR", 
                "Error interno procesando comando de volumen: " + e.getMessage(), 
                session.getId()
            );
            sendMessage(session, errorMessage);
        }
    }
    
    /**
     * Crea un mensaje de error estructurado
     */
    private String createErrorResponse(String errorCode, String message, String sessionId) {
        try {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "error");
            errorResponse.put("errorCode", errorCode);
            errorResponse.put("message", message);
            errorResponse.put("sessionId", sessionId);
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return objectMapper.writeValueAsString(errorResponse);
        } catch (Exception e) {
            // Fallback en caso de error al crear el JSON
            return String.format(
                "{\"type\":\"error\",\"errorCode\":\"%s\",\"message\":\"%s\",\"sessionId\":\"%s\",\"timestamp\":\"%s\"}",
                errorCode, 
                message.replace("\\", "\\\\").replace("\"", "\\\""), 
                sessionId, 
                java.time.LocalDateTime.now().toString()
            );
        }
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