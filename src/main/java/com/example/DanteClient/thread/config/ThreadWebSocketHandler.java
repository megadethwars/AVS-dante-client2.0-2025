package com.example.DanteClient.thread.config;

import com.example.DanteClient.thread.service.ChannelThreadService;
import com.example.DanteClient.thread.model.ChannelThread;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Handler de WebSocket para notificaciones de threads
 * Endpoint: ws://localhost:8080/thread
 */
@Component
public class ThreadWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    @Lazy
    private ChannelThreadService threadService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Set de sesiones activas para broadcast
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        
        System.out.println("======= NEW THREAD WEBSOCKET CONNECTION =======");
        System.out.println("🧵 Session ID: " + session.getId());
        System.out.println("📍 Remote Address: " + session.getRemoteAddress());
        System.out.println("🌐 URI: " + session.getUri());
        System.out.println("📊 Total conexiones thread activas: " + sessions.size());
        System.out.println("⏰ Conectado en: " + java.time.LocalDateTime.now());
        System.out.println("===============================================");
        
        // Enviar mensaje de bienvenida con estado actual de threads
        List<ChannelThread> activeThreads = threadService.getAllActiveThreads();
        
        Map<String, Object> welcomeMessage = new HashMap<>();
        welcomeMessage.put("type", "connection");
        welcomeMessage.put("status", "connected");
        welcomeMessage.put("sessionId", session.getId());
        welcomeMessage.put("totalConnections", sessions.size());
        welcomeMessage.put("activeThreadsCount", activeThreads.size());
        welcomeMessage.put("activeThreads", activeThreads.stream().map(thread -> {
            Map<String, Object> threadInfo = new HashMap<>();
            threadInfo.put("channelId", thread.getChannelId());
            threadInfo.put("channelName", thread.getChannelName());
            threadInfo.put("status", thread.getStatus());
            threadInfo.put("volume", thread.getVolume());
            return threadInfo;
        }).toList());
        welcomeMessage.put("timestamp", java.time.LocalDateTime.now().toString());
        
        String welcomeJson = objectMapper.writeValueAsString(welcomeMessage);
        sendMessage(session, welcomeJson);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        
        System.out.println("====== THREAD WEBSOCKET DISCONNECTION ======");
        System.out.println("❌ Session ID cerrada: " + session.getId());
        System.out.println("📍 Remote Address: " + session.getRemoteAddress());
        System.out.println("🔍 Close Status: " + status.getCode() + " - " + status.getReason());
        System.out.println("📊 Conexiones thread restantes: " + sessions.size());
        System.out.println("⏰ Desconectado en: " + java.time.LocalDateTime.now());
        System.out.println("============================================");
    }
    
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        System.out.println("===== THREAD WEBSOCKET MESSAGE RECEIVED =====");
        System.out.println("🧵 Session ID: " + session.getId());
        System.out.println("📨 Raw Message: " + payload);
        
        try {
            // Parsear el mensaje JSON
            JsonNode jsonNode = objectMapper.readTree(payload);
            
            // Procesar comandos específicos
            if (jsonNode.has("command")) {
                String command = jsonNode.get("command").asText();
                processCommand(session, command, jsonNode);
            } else {
                // Comando no reconocido
                String errorMessage = createErrorResponse("UNKNOWN_COMMAND", "Comando no reconocido. Comandos disponibles: status, list", session.getId());
                sendMessage(session, errorMessage);
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ Error al procesar mensaje thread: " + e.getMessage());
            String errorMessage = createErrorResponse("JSON_PARSE_ERROR", "Error al parsear JSON: " + e.getMessage(), session.getId());
            sendMessage(session, errorMessage);
        }
        
        System.out.println("==============================================");
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("❌ Error en Thread WebSocket " + session.getId() + ": " + exception.getMessage());
        if (session.isOpen()) {
            session.close();
        }
        sessions.remove(session);
    }
    
    /**
     * Procesa comandos específicos del cliente
     */
    private void processCommand(WebSocketSession session, String command, JsonNode jsonNode) {
        try {
            switch (command.toLowerCase()) {
                case "status":
                    sendThreadStatus(session);
                    break;
                case "list":
                    sendActiveThreadsList(session);
                    break;
                default:
                    String errorMessage = createErrorResponse("INVALID_COMMAND", "Comando '" + command + "' no válido", session.getId());
                    sendMessage(session, errorMessage);
            }
        } catch (Exception e) {
            String errorMessage = createErrorResponse("COMMAND_ERROR", "Error ejecutando comando: " + e.getMessage(), session.getId());
            sendMessage(session, errorMessage);
        }
    }
    
    /**
     * Envía el estado general de threads
     */
    private void sendThreadStatus(WebSocketSession session) {
        try {
            int activeCount = threadService.getActiveThreadsCount();
            List<ChannelThread> activeThreads = threadService.getAllActiveThreads();
            
            Map<String, Object> statusResponse = new HashMap<>();
            statusResponse.put("type", "thread_status");
            statusResponse.put("activeThreadsCount", activeCount);
            statusResponse.put("totalConnections", sessions.size());
            statusResponse.put("threads", activeThreads.stream().map(thread -> {
                Map<String, Object> threadInfo = new HashMap<>();
                threadInfo.put("channelId", thread.getChannelId());
                threadInfo.put("channelName", thread.getChannelName());
                threadInfo.put("status", thread.getStatus());
                threadInfo.put("currentTask", thread.getCurrentTask());
                threadInfo.put("volume", thread.getVolume());
                threadInfo.put("startTime", thread.getStartTime().toString());
                threadInfo.put("isRunning", thread.isRunning());
                return threadInfo;
            }).toList());
            statusResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            String statusJson = objectMapper.writeValueAsString(statusResponse);
            sendMessage(session, statusJson);
            
        } catch (Exception e) {
            String errorMessage = createErrorResponse("STATUS_ERROR", "Error obteniendo estado: " + e.getMessage(), session.getId());
            sendMessage(session, errorMessage);
        }
    }
    
    /**
     * Envía lista de threads activos
     */
    private void sendActiveThreadsList(WebSocketSession session) {
        try {
            List<ChannelThread> activeThreads = threadService.getAllActiveThreads();
            
            Map<String, Object> listResponse = new HashMap<>();
            listResponse.put("type", "thread_list");
            listResponse.put("count", activeThreads.size());
            listResponse.put("threads", activeThreads.stream().map(thread -> {
                Map<String, Object> threadInfo = new HashMap<>();
                threadInfo.put("channelId", thread.getChannelId());
                threadInfo.put("channelName", thread.getChannelName());
                threadInfo.put("status", thread.getStatus());
                threadInfo.put("volume", thread.getVolume());
                return threadInfo;
            }).toList());
            listResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            
            String listJson = objectMapper.writeValueAsString(listResponse);
            sendMessage(session, listJson);
            
        } catch (Exception e) {
            String errorMessage = createErrorResponse("LIST_ERROR", "Error obteniendo lista: " + e.getMessage(), session.getId());
            sendMessage(session, errorMessage);
        }
    }
    
    /**
     * Notifica cuando un thread se inicia
     */
    public void notifyThreadStarted(int channelId, String channelName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "thread_started");
        notification.put("channelId", channelId);
        notification.put("channelName", channelName);
        notification.put("message", "Thread iniciado para canal " + channelId + " (" + channelName + ")");
        notification.put("timestamp", java.time.LocalDateTime.now().toString());
        
        broadcastNotification(notification);
        System.out.println("🚀 Notificación broadcast: Thread " + channelId + " iniciado");
    }
    
    /**
     * Notifica cuando un thread finaliza
     */
    public void notifyThreadFinished(int channelId, String channelName, String reason) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "thread_finished");
        notification.put("channelId", channelId);
        notification.put("channelName", channelName);
        notification.put("reason", reason);
        notification.put("message", "Thread finalizado para canal " + channelId + " (" + channelName + ") - Razón: " + reason);
        notification.put("timestamp", java.time.LocalDateTime.now().toString());
        
        broadcastNotification(notification);
        System.out.println("🏁 Notificación broadcast: Thread " + channelId + " finalizado - " + reason);
    }
    
    /**
     * Notifica cuando ocurre una excepción en un thread
     */
    public void notifyThreadException(int channelId, String channelName, String exceptionType, String errorMessage) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "thread_exception");
        notification.put("channelId", channelId);
        notification.put("channelName", channelName);
        notification.put("exceptionType", exceptionType);
        notification.put("errorMessage", errorMessage);
        notification.put("message", "Excepción en thread del canal " + channelId + " (" + channelName + "): " + errorMessage);
        notification.put("timestamp", java.time.LocalDateTime.now().toString());
        
        broadcastNotification(notification);
        System.out.println("⚠️ Notificación broadcast: Excepción en thread " + channelId + " - " + exceptionType);
    }
    
    /**
     * Notifica cambio de estado en un thread
     */
    public void notifyThreadStatusChange(int channelId, String channelName, String oldStatus, String newStatus) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "thread_status_change");
        notification.put("channelId", channelId);
        notification.put("channelName", channelName);
        notification.put("oldStatus", oldStatus);
        notification.put("newStatus", newStatus);
        notification.put("message", "Estado del thread " + channelId + " cambió de " + oldStatus + " a " + newStatus);
        notification.put("timestamp", java.time.LocalDateTime.now().toString());
        
        broadcastNotification(notification);
        System.out.println("🔄 Notificación broadcast: Thread " + channelId + " cambió a " + newStatus);
    }
    
    /**
     * Envía una notificación a todas las conexiones
     */
    private void broadcastNotification(Map<String, Object> notification) {
        try {
            String notificationJson = objectMapper.writeValueAsString(notification);
            broadcastMessage(notificationJson);
        } catch (Exception e) {
            System.err.println("❌ Error creando notificación JSON: " + e.getMessage());
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
     * Envía un mensaje a una sesión específica
     */
    public void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            System.err.println("Error al enviar mensaje thread a sesión " + session.getId() + ": " + e.getMessage());
            sessions.remove(session);
        }
    }
    
    /**
     * Envía un mensaje a todas las conexiones activas
     */
    public void broadcastMessage(String message) {
        System.out.println("📡 Broadcasting notificación thread: " + message);
        
        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                    return false; // Mantener sesión
                } else {
                    return true; // Remover sesión cerrada
                }
            } catch (IOException e) {
                System.err.println("Error al enviar broadcast thread a sesión " + session.getId() + ": " + e.getMessage());
                return true; // Remover sesión con error
            }
        });
        
        System.out.println("📊 Notificación thread enviada a " + sessions.size() + " conexiones");
    }
    
    /**
     * Obtiene el número de conexiones activas
     */
    public int getActiveConnectionsCount() {
        return sessions.size();
    }
    
    /**
     * Envía estadísticas de conexiones thread
     */
    public void broadcastConnectionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("type", "connection_stats");
        stats.put("activeConnections", sessions.size());
        stats.put("activeThreads", threadService.getActiveThreadsCount());
        stats.put("timestamp", java.time.LocalDateTime.now().toString());
        
        broadcastNotification(stats);
    }
}