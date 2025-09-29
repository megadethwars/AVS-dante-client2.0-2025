package com.example.DanteClient.data.controller;

import com.example.DanteClient.data.model.DanteConfig;
import com.example.DanteClient.data.model.Channel;
import com.example.DanteClient.data.service.DanteConfigService;
import com.example.DanteClient.data.exception.ConfigExceptions;
import com.example.DanteClient.data.dto.SuccessResponse;
import com.example.DanteClient.thread.service.ChannelThreadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import java.util.Map;

/**
 * Controlador REST para gestionar la configuración de Dante
 */
@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class DanteConfigController {
    
    @Autowired
    private DanteConfigService configService;

    @Autowired
    private ChannelThreadService channelThreadService;

    /**
     * Obtiene la configuración completa
     */
    @GetMapping
    public ResponseEntity<DanteConfig> getConfig() {
        DanteConfig config = configService.getOrCreateConfig();
        return ResponseEntity.ok(config);
    }
    
    /**
     * Actualiza la configuración completa
     */
    @PutMapping
    public ResponseEntity<SuccessResponse> updateConfig(@RequestBody DanteConfig config) {
        // El service lanzará excepciones que serán manejadas por ConfigExceptionHandler
        configService.saveConfig(config);
        return ResponseEntity.ok(new SuccessResponse("Configuración actualizada exitosamente", config));
    }
    
    /**
     * Actualiza una propiedad específica de la configuración
     */
    @PatchMapping("/property/{propertyName}")
    public ResponseEntity<SuccessResponse> updateProperty(
            @PathVariable String propertyName,
            @RequestBody Map<String, Object> payload) {
        
        Object value = payload.get("value");
        if (value == null) {
            throw new ConfigExceptions.InvalidConfigValueException("value", null, "non-null value");
        }
        
        // El service lanzará excepciones que serán manejadas por ConfigExceptionHandler
        configService.updateConfigProperty(propertyName, value);
        
        Map<String, Object> responseData = Map.of(
            "property", propertyName,
            "newValue", value
        );
        
        return ResponseEntity.ok(new SuccessResponse(
            "Propiedad '" + propertyName + "' actualizada exitosamente", 
            responseData
        ));
    }
    
    /**
     * Obtiene todos los canales
     */
    @GetMapping("/channels")
    public ResponseEntity<?> getChannels() {
        DanteConfig config = configService.getOrCreateConfig();
        return ResponseEntity.ok(config.getChannels());
    }
    
    /**
     * Añade un nuevo canal
     */
    @PostMapping("/channels")
    public ResponseEntity<SuccessResponse> addChannel(@RequestBody Channel channel) {
        // Validaciones básicas
        if (channel.getName() == null || channel.getName().trim().isEmpty()) {
            throw new ConfigExceptions.InvalidChannelDataException("name", "El nombre del canal no puede estar vacío");
        }
        
        // El service lanzará excepciones que serán manejadas por ConfigExceptionHandler
        configService.addChannel(channel.getName(), channel.isEnabled());
        
        Map<String, Object> responseData = Map.of(
            "channelName", channel.getName(),
            "enabled", channel.isEnabled(),
            "action", "created"
        );
        
        return ResponseEntity.ok(new SuccessResponse("Canal agregado exitosamente", responseData));
    }
    
    /**
     * Obtiene un canal específico por ID
     */
    @GetMapping("/channels/{id}")
    public ResponseEntity<?> getChannelById(@PathVariable int id) {
        Channel channel = configService.getChannelById(id);
        if (channel != null) {
            return ResponseEntity.ok(channel);
        } else {
            throw new ConfigExceptions.ChannelNotFoundException(id);
        }
    }
    
    /**
     * Actualiza un canal específico por ID
     */
    @PutMapping("/channels/{id}")
    public ResponseEntity<SuccessResponse> updateChannelById(
            @PathVariable int id,
            @RequestBody Channel channel) {
        
        // Validaciones básicas
        if (channel.getName() != null && channel.getName().trim().isEmpty()) {
            throw new ConfigExceptions.InvalidChannelDataException("name", "El nombre del canal no puede estar vacío");
        }
        
        // El service lanzará excepciones que serán manejadas por ConfigExceptionHandler
        configService.updateChannelById(id, channel.getName(), channel.isEnabled());
        
        Map<String, Object> responseData = Map.of(
            "channelId", id,
            "updatedName", channel.getName() != null ? channel.getName() : "unchanged",
            "updatedEnabled", channel.isEnabled(),
            "action", "updated"
        );
        
        return ResponseEntity.ok(new SuccessResponse(
            "Canal con ID " + id + " actualizado exitosamente", 
            responseData
        ));
    }
    
    /**
     * Elimina un canal por ID
     */
    @DeleteMapping("/channels/{id}")
    public ResponseEntity<SuccessResponse> removeChannelById(@PathVariable int id) {
        // El service lanzará excepciones que serán manejadas por ConfigExceptionHandler
        configService.removeChannelById(id);
        
        Map<String, Object> responseData = Map.of(
            "channelId", id,
            "action", "deleted"
        );
        
        return ResponseEntity.ok(new SuccessResponse(
            "Canal con ID " + id + " eliminado exitosamente", 
            responseData
        ));
    }
    
    /**
     * Reinicia la configuración a los valores por defecto
     */
    @PostMapping("/reset")
    public ResponseEntity<SuccessResponse> resetConfig() {
        DanteConfig defaultConfig = configService.createDefaultConfig();
        return ResponseEntity.ok(new SuccessResponse(
            "Configuración reiniciada a valores por defecto", 
            defaultConfig
        ));
    }
    
    /**
     * Endpoint de prueba para verificar que el servicio está funcionando
     */
    @GetMapping("/status")
    public ResponseEntity<SuccessResponse> getStatus() {
        Map<String, Object> statusData = Map.of(
            "service", "DanteConfigService",
            "status", "active",
            "version", "1.0.0"
        );
        
        return ResponseEntity.ok(new SuccessResponse(
            "Servicio de configuración Dante funcionando correctamente", 
            statusData
        ));
    }

    /**
     * Obtiene todos los canales con su estado de ejecución
     * GET /api/config/channels/status
     */
    @GetMapping("/channels/status")
    public ResponseEntity<?> getChannelsWithThreadStatus() {
        try {
            DanteConfig config = configService.getOrCreateConfig();
            List<Map<String, Object>> channelsStatus = config.getChannels().stream()
                .map(channel -> {
                    HashMap<String, Object> channelInfo = new HashMap<>();
                    channelInfo.put("id", channel.getId());
                    channelInfo.put("name", channel.getName());
                    channelInfo.put("enabled", channel.isEnabled());
                    
                    // Obtener estado del thread
                    boolean isThreadRunning = channelThreadService.getChannelThreadById(channel.getId()) != null;
                    channelInfo.put("isRunning", isThreadRunning);
                    
                    // Solo incluir startTime si el thread está activo
                    if (isThreadRunning) {
                        channelInfo.put("lastStartTime", 
                            channelThreadService.getChannelThreadById(channel.getId())
                                .getStartTime()
                                .toString());
                    }
                    
                    return channelInfo;
                })
                .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("totalChannels", channelsStatus.size());
            response.put("activeThreads", channelsStatus.stream()
                .filter(channel -> (boolean) channel.get("isRunning"))
                .count());
            response.put("channels", channelsStatus);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            HashMap<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error al obtener el estado de los canales: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}