package com.example.DanteClient.data.service;

import com.example.DanteClient.data.model.DanteConfig;
import com.example.DanteClient.data.model.Channel;
import com.example.DanteClient.data.util.ConfigUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Servicio para gestionar la configuración de Dante almacenada en JSON
 * Actúa como una "base de datos" basada en archivo
 */
@Service
public class DanteConfigService {
    
    private static final String CONFIG_FILE_NAME = "config_dante.json";
    private final ObjectMapper objectMapper;
    private final Path configFilePath;
    
    public DanteConfigService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Determinar la ruta del archivo de configuración
        this.configFilePath = determineConfigPath();
    }
    
    /**
     * Inicializa el servicio al arrancar la aplicación
     * Lee el archivo de configuración y lo sincroniza con el singleton
     */
    @PostConstruct
    public void initializeConfiguration() {
        System.out.println("=== INICIALIZANDO CONFIGURACIÓN DE DANTE ===");
        
        try {
            // Leer configuración desde archivo
            Optional<DanteConfig> configOpt = readConfig();
            
            if (configOpt.isPresent()) {
                // Si existe configuración, sincronizar con RAM
                DanteConfig config = configOpt.get();
                ConfigUtil.updateConfigInMemory(config);
                System.out.println("✅ Configuración cargada desde archivo y sincronizada con RAM");
                System.out.println("   - Servidor: " + config.getServer() + ":" + config.getPort());
                System.out.println("   - Canales: " + config.getChannels().size());
                System.out.println("   - Frecuencia: " + config.getFrequency() + " Hz");
            } else {
                // Si no existe configuración, crear por defecto
                System.out.println("⚠️  Archivo de configuración no encontrado, creando configuración por defecto");
                DanteConfig defaultConfig = createDefaultConfig();
                System.out.println("✅ Configuración por defecto creada y sincronizada");
                System.out.println("   - Servidor: " + defaultConfig.getServer() + ":" + defaultConfig.getPort());
                System.out.println("   - Canales: " + defaultConfig.getChannels().size());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error durante la inicialización de configuración: " + e.getMessage());
            
            // En caso de error, crear configuración por defecto
            try {
                createDefaultConfig();
                System.out.println("✅ Configuración por defecto creada como respaldo");
            } catch (Exception ex) {
                System.err.println("❌ Error crítico al crear configuración por defecto: " + ex.getMessage());
            }
        }
        
        System.out.println("============================================");
    }
    
    /**
     * Determina la ruta del archivo de configuración
     */
    private Path determineConfigPath() {
        try {
            // Primero intentar desde resources
            ClassPathResource resource = new ClassPathResource(CONFIG_FILE_NAME);
            if (resource.exists()) {
                // Si existe en resources, usamos la ruta del directorio de trabajo + resources
                Path resourcesPath = Paths.get("src", "main", "resources", CONFIG_FILE_NAME);
                if (Files.exists(resourcesPath)) {
                    return resourcesPath;
                }
            }
        } catch (Exception e) {
            System.out.println("No se pudo acceder a resources, usando directorio de trabajo");
        }
        
        // Si no existe, crear en el directorio de trabajo
        return Paths.get(CONFIG_FILE_NAME);
    }
    
    /**
     * Lee la configuración desde el archivo JSON
     */
    public Optional<DanteConfig> readConfig() {
        try {
            if (Files.exists(configFilePath)) {
                DanteConfig config = objectMapper.readValue(configFilePath.toFile(), DanteConfig.class);
                return Optional.of(config);
            } else {
                System.out.println("Archivo de configuración no encontrado: " + configFilePath);
                return Optional.empty();
            }
        } catch (IOException e) {
            System.err.println("Error al leer la configuración: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Guarda la configuración en el archivo JSON y actualiza RAM
     */
    public boolean saveConfig(DanteConfig config) {
        try {
            // Crear directorios padre si no existen
            Files.createDirectories(configFilePath.getParent());
            
            objectMapper.writeValue(configFilePath.toFile(), config);
            System.out.println("Configuración guardada en: " + configFilePath);
            
            // Sincronizar con RAM para lectura rápida
            ConfigUtil.updateConfigInMemory(config);
            
            return true;
        } catch (IOException e) {
            System.err.println("Error al guardar la configuración: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Actualiza una propiedad específica de la configuración
     */
    public boolean updateConfigProperty(String property, Object value) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isPresent()) {
            DanteConfig config = configOpt.get();
            
            switch (property.toLowerCase()) {
                case "server":
                    config.setServer((String) value);
                    break;
                case "port":
                    config.setPort((String) value);
                    break;
                case "multicast_address":
                    config.setMulticastAddress((String) value);
                    break;
                case "multicast_port":
                    config.setMulticastPort((String) value);
                    break;
                case "chunk_size":
                    config.setChunkSize((String) value);
                    break;
                case "timeout":
                    config.setTimeout((String) value);
                    break;
                case "channel_numbers":
                    config.setChannelNumbers((Integer) value);
                    break;
                case "frequency":
                    config.setFrequency((Integer) value);
                    break;
                default:
                    System.err.println("Propiedad no reconocida: " + property);
                    return false;
            }
            
            return saveConfig(config);
        }
        return false;
    }
    
    /**
     * Añade un nuevo canal a la configuración
     */
    public boolean addChannel(String name, boolean enabled) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isPresent()) {
            DanteConfig config = configOpt.get();
            config.addChannel(new Channel(name, enabled));
            return saveConfig(config);
        }
        return false;
    }
    
    /**
     * Añade un canal con ID específico
     */
    public boolean addChannel(int id, String name, boolean enabled) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isPresent()) {
            DanteConfig config = configOpt.get();
            // Verificar que el ID no exista ya
            if (config.getChannelById(id) != null) {
                System.err.println("Ya existe un canal con ID: " + id);
                return false;
            }
            config.addChannel(new Channel(id, name, enabled));
            return saveConfig(config);
        }
        return false;
    }
    
    /**
     * Actualiza un canal específico por ID
     */
    public boolean updateChannelById(int id, String name, Boolean enabled) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isPresent()) {
            DanteConfig config = configOpt.get();
            boolean success = config.updateChannelById(id, name, enabled);
            if (success) {
                return saveConfig(config);
            }
        }
        return false;
    }
    
    /**
     * Elimina un canal por ID
     */
    public boolean removeChannelById(int id) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isPresent()) {
            DanteConfig config = configOpt.get();
            boolean success = config.removeChannelById(id);
            if (success) {
                return saveConfig(config);
            }
        }
        return false;
    }
    
    /**
     * Obtiene un canal específico por ID
     */
    public Channel getChannelById(int id) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isPresent()) {
            return configOpt.get().getChannelById(id);
        }
        return null;
    }
    
    /**
     * Actualiza un canal específico por índice (método legacy)
     */
    public boolean updateChannel(int index, String name, Boolean enabled) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isPresent()) {
            DanteConfig config = configOpt.get();
            Channel channel = config.getChannel(index);
            if (channel != null) {
                if (name != null) {
                    channel.setName(name);
                }
                if (enabled != null) {
                    channel.setEnabled(enabled);
                }
                return saveConfig(config);
            }
        }
        return false;
    }
    
    /**
     * Elimina un canal por índice (método legacy)
     */
    public boolean removeChannel(int index) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isPresent()) {
            DanteConfig config = configOpt.get();
            config.removeChannel(index);
            return saveConfig(config);
        }
        return false;
    }
    
    /**
     * Crea una configuración por defecto si no existe
     */
    public DanteConfig createDefaultConfig() {
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
        
        // Añadir algunos canales por defecto
        defaultConfig.addChannel(new Channel(1, "Channel 1", true));
        defaultConfig.addChannel(new Channel(2, "Channel 2", false));
        defaultConfig.addChannel(new Channel(3, "Channel 3", true));
        
        // Guardar en archivo y sincronizar con RAM
        saveConfig(defaultConfig);
        return defaultConfig;
    }
    
    /**
     * Obtiene la configuración, creando una por defecto si no existe
     */
    public DanteConfig getOrCreateConfig() {
        return readConfig().orElse(createDefaultConfig());
    }
    
    /**
     * Sincroniza manualmente el archivo con RAM
     */
    public boolean syncWithRAM() {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isPresent()) {
            ConfigUtil.updateConfigInMemory(configOpt.get());
            System.out.println("Configuración sincronizada manualmente con RAM");
            return true;
        }
        return false;
    }
    
    /**
     * Recarga la configuración desde archivo a RAM
     */
    public void reloadConfigFromFile() {
        ConfigUtil.reloadFromFile();
        System.out.println("Configuración recargada desde archivo a RAM");
    }
}