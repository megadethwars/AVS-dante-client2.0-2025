package com.example.DanteClient.data.controller;

import com.example.DanteClient.data.model.DanteConfig;
import com.example.DanteClient.data.model.Channel;
import com.example.DanteClient.data.service.DanteConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<String> updateConfig(@RequestBody DanteConfig config) {
        boolean success = configService.saveConfig(config);
        if (success) {
            return ResponseEntity.ok("Configuración actualizada exitosamente");
        } else {
            return ResponseEntity.internalServerError()
                    .body("Error al actualizar la configuración");
        }
    }
    
    /**
     * Actualiza una propiedad específica de la configuración
     */
    @PatchMapping("/property/{propertyName}")
    public ResponseEntity<String> updateProperty(
            @PathVariable String propertyName,
            @RequestBody Map<String, Object> payload) {
        
        Object value = payload.get("value");
        if (value == null) {
            return ResponseEntity.badRequest()
                    .body("El campo 'value' es requerido");
        }
        
        boolean success = configService.updateConfigProperty(propertyName, value);
        if (success) {
            return ResponseEntity.ok("Propiedad '" + propertyName + "' actualizada exitosamente");
        } else {
            return ResponseEntity.badRequest()
                    .body("Error al actualizar la propiedad '" + propertyName + "'");
        }
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
    public ResponseEntity<String> addChannel(@RequestBody Channel channel) {
        boolean success = configService.addChannel(channel.getName(), channel.isEnabled());
        if (success) {
            return ResponseEntity.ok("Canal agregado exitosamente");
        } else {
            return ResponseEntity.internalServerError()
                    .body("Error al agregar el canal");
        }
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
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Actualiza un canal específico por ID
     */
    @PutMapping("/channels/{id}")
    public ResponseEntity<String> updateChannelById(
            @PathVariable int id,
            @RequestBody Channel channel) {
        
        boolean success = configService.updateChannelById(id, channel.getName(), channel.isEnabled());
        if (success) {
            return ResponseEntity.ok("Canal con ID " + id + " actualizado exitosamente");
        } else {
            return ResponseEntity.badRequest()
                    .body("Error al actualizar el canal con ID " + id + ". Puede que no exista.");
        }
    }
    
    /**
     * Elimina un canal por ID
     */
    @DeleteMapping("/channels/{id}")
    public ResponseEntity<String> removeChannelById(@PathVariable int id) {
        boolean success = configService.removeChannelById(id);
        if (success) {
            return ResponseEntity.ok("Canal con ID " + id + " eliminado exitosamente");
        } else {
            return ResponseEntity.badRequest()
                    .body("Error al eliminar el canal con ID " + id + ". Puede que no exista.");
        }
    }
    
    /**
     * Reinicia la configuración a los valores por defecto
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetConfig() {
        configService.createDefaultConfig();
        return ResponseEntity.ok("Configuración reiniciada a valores por defecto");
    }
    
    /**
     * Endpoint de prueba para verificar que el servicio está funcionando
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Servicio de configuración Dante funcionando correctamente");
    }
}