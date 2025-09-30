package com.example.DanteClient.thread.controller;

import com.example.DanteClient.thread.model.ChannelVolume;
import com.example.DanteClient.thread.config.VolumeWebSocketHandler;
import com.example.DanteClient.data.util.ConfigUtil;
import com.example.DanteClient.data.model.Channel;
import com.example.DanteClient.thread.service.ChannelVolumeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controlador REST para gestionar volumen de canales con WebSocket
 */
@RestController
@RequestMapping("/api/volume")
@CrossOrigin(origins = "*")
public class ChannelVolumeController {
    
    @Autowired
    private VolumeWebSocketHandler webSocketHandler;

    @Autowired
    private ChannelVolumeManager volumeManager;
    
    // Almacenamiento en memoria de los niveles de volumen actuales
    private final ConcurrentHashMap<Integer, ChannelVolume> channelVolumes = new ConcurrentHashMap<>();
    
    /**
     * Establece el volumen de un canal específico
     * POST /api/volume/channel/{channelId}
     */
    @PostMapping("/channel/{channelId}")
    public ResponseEntity<?> setChannelVolume(
            @PathVariable int channelId, 
            @RequestBody Map<String, Object> payload) {
        
        // Validar que el canal existe en la configuración
        Channel channel = ConfigUtil.getChannelById(channelId);
        if (channel == null) {
            return ResponseEntity.badRequest()
                    .body("Canal con ID " + channelId + " no encontrado en la configuración");
        }
        
        // Extraer nivel de volumen del payload
        Object volumeObj = payload.get("volumeLevel");
        if (volumeObj == null) {
            return ResponseEntity.badRequest()
                    .body("El campo 'volumeLevel' es requerido");
        }
        
        int volumeLevel;
        try {
            volumeLevel = Integer.parseInt(volumeObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body("El campo 'volumeLevel' debe ser un número entero");
        }
        
        // Validar rango (0-100)
        if (volumeLevel < 0 || volumeLevel > 100) {
            return ResponseEntity.badRequest()
                    .body("El nivel de volumen debe estar entre 0 y 100");
        }
        
        // Almacenar volumen en el manager
        volumeManager.setVolume(channelId, volumeLevel);
        
        // Enviar por WebSocket a todos los clientes conectados
        webSocketHandler.broadcastVolumeUpdate(channelId, volumeLevel);
        
        // Respuesta exitosa
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Volumen establecido exitosamente");
        response.put("channelId", channelId);
        response.put("channelName", channel.getName());
        response.put("volumeLevel", volumeLevel);
        response.put("volumeDescription", getVolumeDescription(volumeLevel));
        response.put("timestamp", java.time.LocalDateTime.now());
        response.put("webSocketClients", webSocketHandler.getActiveConnectionsCount());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene el volumen actual de un canal específico
     * GET /api/volume/channel/{channelId}
     */
    @GetMapping("/channel/{channelId}")
    public ResponseEntity<?> getChannelVolume(@PathVariable int channelId) {
        // Validar que el canal existe
        Channel channel = ConfigUtil.getChannelById(channelId);
        if (channel == null) {
            return ResponseEntity.badRequest()
                    .body("Canal con ID " + channelId + " no encontrado en la configuración");
        }
        
        
        
        int volumeLevel = volumeManager.getVolume(channelId);
        Map<String, Object> response = new HashMap<>();
        response.put("channelId", channelId);
        response.put("channelName", channel.getName());
        response.put("volumeLevel", volumeLevel);
        response.put("volumeDescription", getVolumeDescription(volumeLevel));
        response.put("volumePercentage", volumeLevel);
        response.put("isMuted", volumeLevel == 0);
        response.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
        
    }
    
    /**
     * Obtiene los volúmenes de todos los canales
     * GET /api/volume/channels
     */
    @GetMapping("/channels")
    public ResponseEntity<?> getAllChannelVolumes() {
        Map<String, Object> response = new HashMap<>();
        
        Map<Integer, Object> volumes = new HashMap<>();
        ConcurrentHashMap<Integer, Integer> allVolumes = volumeManager.getAllVolumes();
        
        allVolumes.forEach((channelId, volumeLevel) -> {
            Channel channel = ConfigUtil.getChannelById(channelId);
            
            Map<String, Object> volumeInfo = new HashMap<>();
            volumeInfo.put("channelId", channelId);
            volumeInfo.put("channelName", channel != null ? channel.getName() : "Unknown");
            volumeInfo.put("volumeLevel", volumeLevel);
            volumeInfo.put("volumeDescription", getVolumeDescription(volumeLevel));
            volumeInfo.put("isMuted", volumeLevel == 0);
            volumeInfo.put("timestamp", java.time.LocalDateTime.now());
            
            volumes.put(channelId, volumeInfo);
        });
        
        response.put("volumes", volumes);
        response.put("totalChannels", volumes.size());
        response.put("webSocketClients", webSocketHandler.getActiveConnectionsCount());
        response.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Silencia (mute) un canal específico
     * POST /api/volume/channel/{channelId}/mute
     */
    @PostMapping("/channel/{channelId}/mute")
    public ResponseEntity<?> muteChannel(@PathVariable int channelId) {
        return setVolumeLevel(channelId, 0, "Canal silenciado (mute)");
    }
    
    /**
     * Desmutea un canal a un volumen por defecto
     * POST /api/volume/channel/{channelId}/unmute
     */
    @PostMapping("/channel/{channelId}/unmute")
    public ResponseEntity<?> unmuteChannel(@PathVariable int channelId) {
        return setVolumeLevel(channelId, 50, "Canal desmuteado");
    }
    
    /**
     * Establece volumen al máximo
     * POST /api/volume/channel/{channelId}/max
     */
    @PostMapping("/channel/{channelId}/max")
    public ResponseEntity<?> setMaxVolume(@PathVariable int channelId) {
        return setVolumeLevel(channelId, 100, "Volumen establecido al máximo");
    }
    
    /**
     * Método helper para establecer nivel de volumen
     */
    private ResponseEntity<?> setVolumeLevel(int channelId, int volumeLevel, String action) {
        Channel channel = ConfigUtil.getChannelById(channelId);
        if (channel == null) {
            return ResponseEntity.badRequest()
                    .body("Canal con ID " + channelId + " no encontrado");
        }
        
        ChannelVolume channelVolume = new ChannelVolume(channelId, volumeLevel, channel.getName());
        channelVolumes.put(channelId, channelVolume);
        
        // Enviar por WebSocket
        webSocketHandler.broadcastVolumeUpdate(channelId, volumeLevel);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", action);
        response.put("channelId", channelId);
        response.put("channelName", channel.getName());
        response.put("volumeLevel", volumeLevel);
        response.put("volumeDescription", channelVolume.getVolumeDescription());
        response.put("timestamp", channelVolume.getTimestamp());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene estadísticas de WebSocket
     * GET /api/volume/websocket/stats
     */
    @GetMapping("/websocket/stats")
    public ResponseEntity<?> getWebSocketStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeConnections", webSocketHandler.getActiveConnectionsCount());
        stats.put("totalChannelsWithVolume", channelVolumes.size());
        stats.put("websocketEndpoint", "/ws/volume");
        stats.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Método helper para obtener descripción del volumen
     */
    private String getVolumeDescription(int volumeLevel) {
        if (volumeLevel == 0) return "Silenciado";
        if (volumeLevel <= 25) return "Bajo";
        if (volumeLevel <= 50) return "Medio";
        if (volumeLevel <= 75) return "Alto";
        return "Máximo";
    }

    /**
     * Envía mensaje de prueba por WebSocket
     * POST /api/volume/websocket/test
     */
    @PostMapping("/websocket/test")
    public ResponseEntity<?> sendTestMessage(@RequestBody Map<String, Object> payload) {
        String message = payload.getOrDefault("message", "Mensaje de prueba").toString();
        
        String testJson = String.format(
            "{\"type\":\"test\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            message,
            java.time.LocalDateTime.now().toString()
        );
        
        webSocketHandler.broadcastMessage(testJson);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Mensaje de prueba enviado");
        response.put("sentMessage", message);
        response.put("clientsCount", webSocketHandler.getActiveConnectionsCount());
        
        return ResponseEntity.ok(response);
    }
}