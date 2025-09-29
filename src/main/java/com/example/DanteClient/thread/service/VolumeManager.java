package com.example.DanteClient.thread.service;

import com.example.DanteClient.data.model.Channel;
import com.example.DanteClient.data.service.DanteConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * Singleton que maneja los volúmenes de los canales en memoria
 */
@Component
public class VolumeManager {
    
    private static VolumeManager instance;
    private final ConcurrentHashMap<Integer, Integer> channelVolumes;
    
    @Autowired
    private DanteConfigService configService;
    
    private VolumeManager() {
        this.channelVolumes = new ConcurrentHashMap<>();
    }
    
    /**
     * Obtener la instancia del singleton
     */
    public static VolumeManager getInstance() {
        if (instance == null) {
            synchronized (VolumeManager.class) {
                if (instance == null) {
                    instance = new VolumeManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Inicializar los volúmenes de los canales al arrancar la aplicación
     */
    @PostConstruct
    public void init() {
        // Obtener los canales de la configuración
        List<Channel> channels = configService.getOrCreateConfig().getChannels();
        
        // Inicializar los volúmenes a 0 para cada canal
        channels.forEach(channel -> {
            channelVolumes.put(channel.getId(), 0);
        });
        
        System.out.println("VolumeManager inicializado con " + channels.size() + " canales");
    }
    
    /**
     * Establecer el volumen para un canal específico
     */
    public void setVolume(int channelId, int volume) {
        if (volume < 0 || volume > 100) {
            throw new IllegalArgumentException("El volumen debe estar entre 0 y 100");
        }
        
        channelVolumes.put(channelId, volume);
        System.out.println("Canal " + channelId + " - Volumen establecido a " + volume);
    }
    
    /**
     * Obtener el volumen actual de un canal
     */
    public int getVolume(int channelId) {
        return channelVolumes.getOrDefault(channelId, 0);
    }
    
    /**
     * Verificar si existe un canal
     */
    public boolean hasChannel(int channelId) {
        return channelVolumes.containsKey(channelId);
    }
    
    /**
     * Reiniciar el volumen de un canal a 0
     */
    public void resetVolume(int channelId) {
        channelVolumes.put(channelId, 0);
        System.out.println("Canal " + channelId + " - Volumen reiniciado a 0");
    }
    
    /**
     * Reiniciar todos los volúmenes a 0
     */
    public void resetAllVolumes() {
        channelVolumes.replaceAll((k, v) -> 0);
        System.out.println("Todos los volúmenes reiniciados a 0");
    }
    
    /**
     * Obtener todos los volúmenes actuales
     */
    public ConcurrentHashMap<Integer, Integer> getAllVolumes() {
        return new ConcurrentHashMap<>(channelVolumes);
    }
    
    /**
     * Agregar un nuevo canal (si no existe)
     */
    public void addChannel(int channelId) {
        channelVolumes.putIfAbsent(channelId, 0);
        System.out.println("Nuevo canal " + channelId + " agregado con volumen 0");
    }
    
    /**
     * Eliminar un canal
     */
    public void removeChannel(int channelId) {
        channelVolumes.remove(channelId);
        System.out.println("Canal " + channelId + " eliminado del gestor de volúmenes");
    }
}