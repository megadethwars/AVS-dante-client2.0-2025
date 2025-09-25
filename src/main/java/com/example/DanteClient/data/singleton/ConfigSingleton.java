package com.example.DanteClient.data.singleton;

import com.example.DanteClient.data.model.DanteConfig;
import com.example.DanteClient.data.model.Channel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Singleton para mantener la configuración de Dante en RAM
 * Permite acceso súper rápido a los datos sin leer archivo constantemente
 */
@Component
public class ConfigSingleton {
    
    private static ConfigSingleton instance;
    private DanteConfig configInMemory;
    private final ObjectMapper objectMapper;
    private final Object lock = new Object(); // Para thread safety
    
    public ConfigSingleton() {
        this.objectMapper = new ObjectMapper();
        instance = this; // Asignar instancia para acceso estático
    }
    
    /**
     * Carga la configuración al inicializar el componente
     */
    @PostConstruct
    public void init() {
        loadConfigFromFile();
    }
    
    /**
     * Obtiene la instancia singleton (acceso estático)
     */
    public static ConfigSingleton getInstance() {
        return instance;
    }
    
    /**
     * Obtiene la configuración completa desde RAM
     */
    public DanteConfig getConfig() {
        synchronized (lock) {
            if (configInMemory == null) {
                loadConfigFromFile();
            }
            return configInMemory;
        }
    }
    
    /**
     * Actualiza la configuración en RAM (llamar esto después de guardar en archivo)
     */
    public void updateConfigInMemory(DanteConfig newConfig) {
        synchronized (lock) {
            this.configInMemory = newConfig;
            System.out.println("Configuración actualizada en RAM");
        }
    }
    
    /**
     * Recarga la configuración desde el archivo
     */
    public void reloadFromFile() {
        synchronized (lock) {
            loadConfigFromFile();
        }
    }
    
    // ========== MÉTODOS DE ACCESO RÁPIDO ==========
    
    /**
     * Obtiene el servidor desde RAM
     */
    public String getServer() {
        DanteConfig config = getConfig();
        return config != null ? config.getServer() : "192.168.1.100";
    }
    
    /**
     * Obtiene el puerto desde RAM
     */
    public String getPort() {
        DanteConfig config = getConfig();
        return config != null ? config.getPort() : "8080";
    }
    
    /**
     * Obtiene la dirección multicast desde RAM
     */
    public String getMulticastAddress() {
        DanteConfig config = getConfig();
        return config != null ? config.getMulticastAddress() : "224.0.0.1";
    }
    
    /**
     * Obtiene el puerto multicast desde RAM
     */
    public String getMulticastPort() {
        DanteConfig config = getConfig();
        return config != null ? config.getMulticastPort() : "5000";
    }
    
    /**
     * Obtiene el chunk size desde RAM
     */
    public String getChunkSize() {
        DanteConfig config = getConfig();
        return config != null ? config.getChunkSize() : "1024";
    }
    
    /**
     * Obtiene el timeout desde RAM
     */
    public String getTimeout() {
        DanteConfig config = getConfig();
        return config != null ? config.getTimeout() : "5000";
    }
    
    /**
     * Obtiene el número de canales desde RAM
     */
    public int getChannelNumbers() {
        DanteConfig config = getConfig();
        return config != null ? config.getChannelNumbers() : 64;
    }
    
    /**
     * Obtiene la frecuencia desde RAM
     */
    public int getFrequency() {
        DanteConfig config = getConfig();
        return config != null ? config.getFrequency() : 44100;
    }
    
    /**
     * Obtiene todos los canales desde RAM
     */
    public List<Channel> getAllChannels() {
        DanteConfig config = getConfig();
        return config != null ? config.getChannels() : java.util.Collections.emptyList();
    }
    
    /**
     * Obtiene un canal específico por ID desde RAM
     */
    public Channel getChannelById(int id) {
        DanteConfig config = getConfig();
        return config != null ? config.getChannelById(id) : null;
    }
    
    /**
     * Obtiene solo los canales habilitados desde RAM
     */
    public List<Channel> getEnabledChannels() {
        return getAllChannels().stream()
                .filter(Channel::isEnabled)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Verifica si un canal está habilitado desde RAM
     */
    public boolean isChannelEnabled(int id) {
        Channel channel = getChannelById(id);
        return channel != null && channel.isEnabled();
    }
    
    /**
     * Obtiene el nombre de un canal desde RAM
     */
    public String getChannelName(int id) {
        Channel channel = getChannelById(id);
        return channel != null ? channel.getName() : null;
    }
    
    /**
     * Cuenta el total de canales desde RAM
     */
    public int getTotalChannelsCount() {
        return getAllChannels().size();
    }
    
    /**
     * Cuenta los canales habilitados desde RAM
     */
    public int getEnabledChannelsCount() {
        return (int) getAllChannels().stream()
                .filter(Channel::isEnabled)
                .count();
    }
    
    /**
     * Obtiene información de estado desde RAM
     */
    public String getConfigStatus() {
        DanteConfig config = getConfig();
        if (config == null) {
            return "Configuración no disponible en RAM";
        }
        
        return String.format(
                "RAM - Servidor: %s:%s, Canales: %d/%d habilitados, Frecuencia: %d Hz",
                config.getServer(),
                config.getPort(),
                getEnabledChannelsCount(),
                getTotalChannelsCount(),
                config.getFrequency()
        );
    }
    
    // ========== MÉTODOS PRIVADOS ==========
    
    /**
     * Carga la configuración desde el archivo JSON
     */
    private void loadConfigFromFile() {
        try {
            Path configPath = determineConfigPath();
            if (Files.exists(configPath)) {
                DanteConfig config = objectMapper.readValue(configPath.toFile(), DanteConfig.class);
                this.configInMemory = config;
                System.out.println("Configuración cargada en RAM desde: " + configPath);
            } else {
                System.out.println("Archivo de configuración no encontrado");
                this.configInMemory = createDefaultConfig();
            }
        } catch (IOException e) {
            System.err.println("Error al cargar configuración en RAM: " + e.getMessage());
            this.configInMemory = createDefaultConfig();
        }
    }
    
    /**
     * Determina la ruta del archivo de configuración
     */
    private Path determineConfigPath() {
        try {
            ClassPathResource resource = new ClassPathResource("config_dante.json");
            if (resource.exists()) {
                Path resourcesPath = Paths.get("src", "main", "resources", "config_dante.json");
                if (Files.exists(resourcesPath)) {
                    return resourcesPath;
                }
            }
        } catch (Exception e) {
            System.out.println("Usando directorio de trabajo para config");
        }
        return Paths.get("config_dante.json");
    }
    
    /**
     * Crea configuración por defecto
     */
    private DanteConfig createDefaultConfig() {
        DanteConfig defaultConfig = new DanteConfig(
                "192.168.1.100",
                "8080",
                "224.0.0.1",
                "5000",
                "1024",
                "5000",
                64,
                44100
        );
        
        defaultConfig.addChannel(new Channel(1, "Channel 1", true));
        defaultConfig.addChannel(new Channel(2, "Channel 2", false));
        defaultConfig.addChannel(new Channel(3, "Channel 3", true));
        
        return defaultConfig;
    }
    
    /**
     * Imprime la configuración actual (para debugging)
     */
    public void printConfig() {
        System.out.println("=== CONFIGURACIÓN EN RAM ===");
        DanteConfig config = getConfig();
        if (config != null) {
            System.out.println("Servidor: " + config.getServer() + ":" + config.getPort());
            System.out.println("Multicast: " + config.getMulticastAddress() + ":" + config.getMulticastPort());
            System.out.println("Frecuencia: " + config.getFrequency() + " Hz");
            System.out.println("Canales (" + getTotalChannelsCount() + "):");
            getAllChannels().forEach(channel -> 
                System.out.println("  - ID:" + channel.getId() + " " + 
                                 channel.getName() + " [" + 
                                 (channel.isEnabled() ? "ON" : "OFF") + "]")
            );
        } else {
            System.out.println("No hay configuración en RAM");
        }
        System.out.println("============================");
    }
}