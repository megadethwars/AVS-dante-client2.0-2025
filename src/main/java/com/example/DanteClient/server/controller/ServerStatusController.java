package com.example.DanteClient.server.controller;

import com.example.DanteClient.server.service.ServerPingService;
import com.example.DanteClient.server.model.ServerStatus;
import com.example.DanteClient.data.util.ConfigUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * Controlador REST para verificar el estado del servidor remoto
 */
@RestController
@RequestMapping("/api/server")
@CrossOrigin(origins = "*")
public class ServerStatusController {
    
    @Autowired
    private ServerPingService serverPingService;
    
    /**
     * Obtiene el estado actual del servidor configurado
     * GET /api/server/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getServerStatus() {
        ServerStatus status = serverPingService.pingConfiguredServer();
        
        Map<String, Object> response = new HashMap<>();
        response.put("server", status.getServerAddress());
        response.put("port", status.getPort());
        response.put("fullUrl", status.getFullUrl());
        response.put("isReachable", status.isReachable());
        response.put("status", status.getStatus());
        response.put("responseTimeMs", status.getResponseTimeMs());
        response.put("connectionDescription", status.getConnectionDescription());
        response.put("isFastResponse", status.isFastResponse());
        response.put("timeoutMs", status.getTimeoutMs());
        response.put("lastChecked", status.getLastChecked());
        response.put("errorMessage", status.getErrorMessage());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene información detallada del servidor con configuración
     * GET /api/server/info
     */
    @GetMapping("/info")
    public ResponseEntity<?> getDetailedServerInfo() {
        ServerStatus status = serverPingService.getDetailedServerInfo();
        
        Map<String, Object> response = new HashMap<>();
        
        // Información de conectividad
        response.put("connectivity", Map.of(
            "server", status.getServerAddress(),
            "port", status.getPort(),
            "fullUrl", status.getFullUrl(),
            "isReachable", status.isReachable(),
            "status", status.getStatus(),
            "responseTimeMs", status.getResponseTimeMs(),
            "connectionDescription", status.getConnectionDescription(),
            "isFastResponse", status.isFastResponse(),
            "lastChecked", status.getLastChecked(),
            "errorMessage", status.getErrorMessage()
        ));
        
        // Configuración completa desde singleton
        response.put("configuration", Map.of(
            "server", ConfigUtil.getServer(),
            "port", ConfigUtil.getPort(),
            "multicastAddress", ConfigUtil.getMulticastAddress(),
            "multicastPort", ConfigUtil.getMulticastPort(),
            "timeout", ConfigUtil.getTimeout(),
            "chunkSize", ConfigUtil.getChunkSize(),
            "frequency", ConfigUtil.getFrequency(),
            "channelNumbers", ConfigUtil.getChannelNumbers()
        ));
        
        // Estadísticas de canales
        response.put("channels", Map.of(
            "totalChannels", ConfigUtil.getTotalChannelsCount(),
            "enabledChannels", ConfigUtil.getEnabledChannelsCount(),
            "configStatus", ConfigUtil.getConfigStatus()
        ));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Realiza ping a un servidor específico
     * POST /api/server/ping
     */
    @PostMapping("/ping")
    public ResponseEntity<?> pingSpecificServer(@RequestBody Map<String, String> payload) {
        String serverAddress = payload.get("server");
        String port = payload.get("port");
        
        if (serverAddress == null || port == null) {
            return ResponseEntity.badRequest()
                    .body("Los campos 'server' y 'port' son requeridos");
        }
        
        ServerStatus status = serverPingService.pingServer(serverAddress, port);
        
        Map<String, Object> response = new HashMap<>();
        response.put("requestedServer", serverAddress);
        response.put("requestedPort", port);
        response.put("fullUrl", status.getFullUrl());
        response.put("isReachable", status.isReachable());
        response.put("status", status.getStatus());
        response.put("responseTimeMs", status.getResponseTimeMs());
        response.put("connectionDescription", status.getConnectionDescription());
        response.put("timeoutMs", status.getTimeoutMs());
        response.put("lastChecked", status.getLastChecked());
        response.put("errorMessage", status.getErrorMessage());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Realiza múltiples pings para estadísticas
     * GET /api/server/ping/multiple/{count}
     */
    @GetMapping("/ping/multiple/{count}")
    public ResponseEntity<?> pingMultiple(@PathVariable int count) {
        if (count < 1 || count > 20) {
            return ResponseEntity.badRequest()
                    .body("El número de pings debe estar entre 1 y 20");
        }
        
        ServerStatus status = serverPingService.pingMultiple(count);
        
        Map<String, Object> response = new HashMap<>();
        response.put("server", status.getServerAddress());
        response.put("port", status.getPort());
        response.put("pingCount", count);
        response.put("isReachable", status.isReachable());
        response.put("status", status.getStatus());
        response.put("averageResponseTimeMs", status.getResponseTimeMs());
        response.put("connectionDescription", status.getConnectionDescription());
        response.put("lastChecked", status.getLastChecked());
        response.put("errorMessage", status.getErrorMessage());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verifica solo conectividad básica (ping rápido)
     * GET /api/server/ping/quick
     */
    @GetMapping("/ping/quick")
    public ResponseEntity<?> quickPing() {
        String server = ConfigUtil.getServer();
        String port = ConfigUtil.getPort();
        
        long startTime = System.currentTimeMillis();
        ServerStatus status = serverPingService.pingConfiguredServer();
        long totalTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("server", server);
        response.put("port", port);
        response.put("isOnline", status.isReachable());
        response.put("responseTimeMs", status.getResponseTimeMs());
        response.put("totalCheckTimeMs", totalTime);
        response.put("status", status.getStatus());
        response.put("timestamp", status.getLastChecked());
        
        // Respuesta simple para checks rápidos
        if (status.isReachable()) {
            response.put("message", "Servidor ONLINE");
        } else {
            response.put("message", "Servidor OFFLINE");
            response.put("errorMessage", status.getErrorMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene solo la configuración del servidor sin hacer ping
     * GET /api/server/config
     */
    @GetMapping("/config")
    public ResponseEntity<?> getServerConfig() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("server", ConfigUtil.getServer());
        config.put("port", ConfigUtil.getPort());
        config.put("fullUrl", "http://" + ConfigUtil.getServer() + ":" + ConfigUtil.getPort());
        config.put("timeout", ConfigUtil.getTimeout());
        config.put("multicastAddress", ConfigUtil.getMulticastAddress());
        config.put("multicastPort", ConfigUtil.getMulticastPort());
        config.put("chunkSize", ConfigUtil.getChunkSize());
        config.put("configStatus", ConfigUtil.getConfigStatus());
        config.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * Health check simple para monitoreo
     * GET /api/server/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            ServerStatus status = serverPingService.pingConfiguredServer();
            
            Map<String, Object> health = new HashMap<>();
            health.put("service", "ServerStatusController");
            health.put("status", status.isReachable() ? "UP" : "DOWN");
            health.put("server", status.getServerAddress() + ":" + status.getPort());
            health.put("responseTime", status.getResponseTimeMs() + "ms");
            health.put("timestamp", java.time.LocalDateTime.now());
            
            if (status.isReachable()) {
                return ResponseEntity.ok(health);
            } else {
                health.put("error", status.getErrorMessage());
                return ResponseEntity.status(503).body(health); // Service Unavailable
            }
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("service", "ServerStatusController");
            error.put("status", "ERROR");
            error.put("error", e.getMessage());
            error.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(error);
        }
    }



    @GetMapping("/powerDown")
    public ResponseEntity<?> powerDownSystem() {
        try {
            serverPingService.turnOFFSystem();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Sistema apagado correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

            
}