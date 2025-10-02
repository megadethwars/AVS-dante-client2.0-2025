package com.example.DanteClient.thread.service;

import com.example.DanteClient.data.model.Channel;
import com.example.DanteClient.data.service.DanteConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import com.example.DanteClient.thread.model.ChannelThread;

/**
 * Singleton que maneja los volúmenes de los canales en memoria
 */
@Component
public class ChannelVolumeManager {
    
    private static volatile ChannelVolumeManager instance;
    private final ConcurrentHashMap<Integer, Integer> channelVolumes;
    private final ConcurrentHashMap<Integer, Integer> previousVolumes;
    private final ConcurrentHashMap<Integer, Boolean> threadActiveStates; // true = activo y no muteado
    
    @Autowired
    private DanteConfigService configService;

   
    
    private ChannelVolumeManager() {
        this.channelVolumes = new ConcurrentHashMap<>();
        this.previousVolumes = new ConcurrentHashMap<>();
        this.threadActiveStates = new ConcurrentHashMap<>();
    }
    
    /**
     * Obtener la instancia del singleton
     */
    public static ChannelVolumeManager getInstance() {
        if (instance == null) {
            synchronized (ChannelVolumeManager.class) {
                if (instance == null) {
                    instance = new ChannelVolumeManager();
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
        
        // Inicializar los volúmenes y estados para cada canal
        channels.forEach(channel -> {
            channelVolumes.put(channel.getId(), 0);
            threadActiveStates.put(channel.getId(), false); // Inicialmente todos inactivos
        });
        
        System.out.println("ChannelVolumeManager inicializado con " + channels.size() + " canales");
        
        // Establecer esta instancia como la instancia singleton
        instance = this;
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
     * Obtener el volumen actual de un canal
     */
    public boolean getMutedThreadStatus(int channelId) {
        return threadActiveStates.getOrDefault(channelId, false);
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

    @Autowired
    private ChannelThreadService threadService;

    /**
     * Silencia todos los canales activos excepto el especificado, estableciendo su volumen a 0.
     * Solo silencia los canales que tienen threads activos.
     * @param channelId El ID del canal que no será silenciado.
     * @return true si al menos un canal activo fue silenciado, false en caso contrario.
     */
    public boolean muteAllExcept(int channelId) {
        boolean muted = false;
        previousVolumes.clear(); // Limpiar volúmenes anteriores
        
        // Obtener todos los threads activos
        for (Integer id : channelVolumes.keySet()) {
            // Solo procesar si es diferente al canal especificado
            if (id != channelId) {
                // Verificar si el thread está activo usando ChannelThreadService
                if (threadService.isThreadActive(id)) {
                    // Solo silenciar si el volumen no es 0
                    int currentVolume = channelVolumes.get(id);
                    if (currentVolume != 0) {
                        previousVolumes.put(id, currentVolume); // Guardar volumen anterior
                        setVolume(id, 0); // Usar el método setVolume para mantener consistencia
                        ChannelThread channelThread = threadService.getThreadInfo(id);
                        if (channelThread != null) {
                            channelThread.setVolume(0);
                        }
                        threadActiveStates.put(id, false); // Marcar como muteado
                        muted = true;
                        System.out.println("Canal " + id + " (activo) ha sido silenciado. Volumen anterior: " + currentVolume);
                    }
                } else {
                    threadActiveStates.put(id, false); // Marcar como inactivo
                    System.out.println("Canal " + id + " está inactivo, no se silencia.");
                }
            } else {
                threadActiveStates.put(id, true); // Marcar el canal no muteado como activo
            }
        }
        
        if (muted) {
            System.out.println("Se han silenciado todos los canales activos excepto " + channelId);
        } else {
            System.out.println("No se encontraron otros canales activos para silenciar.");
        }
        
        return muted;
    }

    /**
     * Restaura los volúmenes de los canales que fueron silenciados previamente.
     * @return true si al menos un canal fue restaurado, false si no hay canales para restaurar.
     */
    /**
     * Obtiene el estado de actividad de un thread específico
     * @param channelId ID del canal
     * @return true si el thread está activo y no muteado, false en caso contrario
     */
    public boolean isThreadActive(int channelId) {
        return threadActiveStates.getOrDefault(channelId, false);
    }

    /**
     * Obtiene un mapa con todos los estados de los threads
     * @return Mapa con los estados de los threads
     */
    public ConcurrentHashMap<Integer, Boolean> getThreadStates() {
        return new ConcurrentHashMap<>(threadActiveStates);
    }

    public boolean unmuteChannels() {
        boolean restored = false;
        
        for (Integer id : previousVolumes.keySet()) {
            // Solo restaurar si el thread sigue activo
            if (threadService.isThreadActive(id)) {
                int previousVolume = previousVolumes.get(id);
                setVolume(id, previousVolume); // Usar el método setVolume para mantener consistencia
                ChannelThread channelThread = threadService.getThreadInfo(id);
                if (channelThread != null) {
                    channelThread.setVolume(previousVolume);
                }
                threadActiveStates.put(id, false); 
                restored = true;
                System.out.println("Canal " + id + " restaurado a volumen: " + previousVolume);
            } else {
                threadActiveStates.put(id, false); // Mantener como inactivo
                System.out.println("Canal " + id + " ya no está activo, no se restaura.");
            }
        }
        
        if (restored) {
            System.out.println("Se han restaurado los volúmenes de los canales previamente silenciados.");
        } else {
            System.out.println("No hay canales para restaurar.");
        }
        
        previousVolumes.clear(); // Limpiar el mapa después de restaurar
        return restored;
    }


}