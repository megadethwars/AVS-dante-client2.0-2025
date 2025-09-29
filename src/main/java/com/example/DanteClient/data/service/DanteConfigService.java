package com.example.DanteClient.data.service;

import com.example.DanteClient.data.model.DanteConfig;
import com.example.DanteClient.data.model.Channel;
import com.example.DanteClient.data.util.ConfigUtil;
import com.example.DanteClient.data.exception.ConfigExceptions;
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
        // Siempre usar el directorio de recursos
        Path resourcesPath = Paths.get("src", "main", "resources", CONFIG_FILE_NAME);
        
        // Crear el directorio si no existe
        try {
            Files.createDirectories(resourcesPath.getParent());
        } catch (IOException e) {
            System.err.println("Error creando directorio de recursos: " + e.getMessage());
        }
        
        return resourcesPath;
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
    public void saveConfig(DanteConfig config) {
        try {
            // Crear directorios padre si no existen
            Files.createDirectories(configFilePath.getParent());
            
            objectMapper.writeValue(configFilePath.toFile(), config);
            System.out.println("Configuración guardada en: " + configFilePath);
            
            // Sincronizar con RAM para lectura rápida
            ConfigUtil.updateConfigInMemory(config);
            
        } catch (IOException e) {
            System.err.println("Error al guardar la configuración: " + e.getMessage());
            throw new ConfigExceptions.ConfigFileWriteException(configFilePath.toString(), e);
        }
    }
    
    /**
     * Actualiza una propiedad específica de la configuración
     */
    public void updateConfigProperty(String property, Object value) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isEmpty()) {
            throw new ConfigExceptions.ConfigNotInitializedException();
        }
        
        DanteConfig config = configOpt.get();
        
        try {
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
                    throw new ConfigExceptions.InvalidConfigPropertyException(property);
            }
        } catch (ClassCastException e) {
            throw new ConfigExceptions.InvalidConfigValueException(property, value, "correct type for " + property);
        }
        
        // Actualizar singleton en RAM antes de guardar
        ConfigUtil.updateConfigInMemory(config);
        System.out.println("Propiedad '" + property + "' actualizada en RAM");
        
        saveConfig(config);
    }
    
    /**
     * Añade un nuevo canal a la configuración
     */
    public void addChannel(String name, boolean enabled) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isEmpty()) {
            throw new ConfigExceptions.ConfigNotInitializedException();
        }
        
        DanteConfig config = configOpt.get();
        config.addChannel(new Channel(name, enabled));
        
        // Actualizar singleton en RAM antes de guardar
        ConfigUtil.updateConfigInMemory(config);
        System.out.println("Nuevo canal '" + name + "' añadido en RAM");
        
        saveConfig(config);
    }
    
    /**
     * Añade un canal con ID específico
     */
    public void addChannel(int id, String name, boolean enabled) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isEmpty()) {
            throw new ConfigExceptions.ConfigNotInitializedException();
        }
        
        DanteConfig config = configOpt.get();
        // Verificar que el ID no exista ya
        if (config.getChannelById(id) != null) {
            throw new ConfigExceptions.ChannelAlreadyExistsException(id);
        }
        
        config.addChannel(new Channel(id, name, enabled));
        saveConfig(config);
    }
    
    /**
     * Actualiza un canal específico por ID
     */
    public void updateChannelById(int id, String name, Boolean enabled) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isEmpty()) {
            throw new ConfigExceptions.ConfigNotInitializedException();
        }
        
        DanteConfig config = configOpt.get();
        boolean success = config.updateChannelById(id, name, enabled);
        
        if (!success) {
            throw new ConfigExceptions.ChannelNotFoundException(id);
        }
        
        // Primero actualizar el singleton en RAM
        ConfigUtil.updateConfigInMemory(config);
        System.out.println("Canal ID " + id + " actualizado en RAM");
        
        // Luego guardar en archivo (saveConfig también actualiza RAM pero es buena práctica)
        saveConfig(config);
    }
    
    /**
     * Elimina un canal por ID
     */
    public void removeChannelById(int id) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isEmpty()) {
            throw new ConfigExceptions.ConfigNotInitializedException();
        }
        
        DanteConfig config = configOpt.get();
        boolean success = config.removeChannelById(id);
        
        if (!success) {
            throw new ConfigExceptions.ChannelNotFoundException(id);
        }
        
        // Actualizar singleton en RAM antes de guardar
        ConfigUtil.updateConfigInMemory(config);
        System.out.println("Canal ID " + id + " eliminado de RAM");
        
        saveConfig(config);
    }
    
    /**
     * Obtiene un canal específico por ID
     */
    public Channel getChannelById(int id) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isEmpty()) {
            throw new ConfigExceptions.ConfigNotInitializedException();
        }
        
        Channel channel = configOpt.get().getChannelById(id);
        if (channel == null) {
            throw new ConfigExceptions.ChannelNotFoundException(id);
        }
        
        return channel;
    }
    
    /**
     * Actualiza un canal específico por índice (método legacy)
     * @deprecated Use updateChannelById instead
     */
    @Deprecated
    public void updateChannel(int index, String name, Boolean enabled) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isEmpty()) {
            throw new ConfigExceptions.ConfigNotInitializedException();
        }
        
        DanteConfig config = configOpt.get();
        Channel channel = config.getChannel(index);
        if (channel == null) {
            throw new ConfigExceptions.ChannelNotFoundException("Index: " + index);
        }
        
        if (name != null) {
            channel.setName(name);
        }
        if (enabled != null) {
            channel.setEnabled(enabled);
        }
        
        // Actualizar singleton en RAM antes de guardar
        ConfigUtil.updateConfigInMemory(config);
        System.out.println("Canal índice " + index + " actualizado en RAM");
        
        saveConfig(config);
    }
    
    /**
     * Elimina un canal por índice (método legacy)
     * @deprecated Use removeChannelById instead
     */
    @Deprecated
    public void removeChannel(int index) {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isEmpty()) {
            throw new ConfigExceptions.ConfigNotInitializedException();
        }
        
        DanteConfig config = configOpt.get();
        
        // Verificar que el índice sea válido antes de eliminar
        if (index < 0 || index >= config.getChannels().size()) {
            throw new ConfigExceptions.ChannelNotFoundException("Index: " + index);
        }
        
        config.removeChannel(index);
        saveConfig(config);
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
        
        try {
            // Guardar en archivo y sincronizar con RAM
            saveConfig(defaultConfig);
        } catch (ConfigExceptions.ConfigFileWriteException e) {
            System.err.println("Warning: Could not save default config to file, using RAM only");
            ConfigUtil.updateConfigInMemory(defaultConfig);
        }
        return defaultConfig;
    }
    
    /**
     * Obtiene la configuración, creando una por defecto si no existe
     */
    public DanteConfig getOrCreateConfig() {
        Optional<DanteConfig> existingConfig = readConfig();
        if (existingConfig.isPresent()) {
            return existingConfig.get();
        }
        
        // Si no existe configuración, crear una por defecto y guardarla
        DanteConfig defaultConfig = createDefaultConfig();
        saveConfig(defaultConfig);
        return defaultConfig;
    }
    
    /**
     * Sincroniza manualmente el archivo con RAM
     */
    public void syncWithRAM() {
        Optional<DanteConfig> configOpt = readConfig();
        if (configOpt.isEmpty()) {
            throw new ConfigExceptions.ConfigFileNotFoundException(configFilePath.toString());
        }
        
        ConfigUtil.updateConfigInMemory(configOpt.get());
        System.out.println("Configuración sincronizada manualmente con RAM");
    }
    
    /**
     * Recarga la configuración desde archivo a RAM
     */
    public void reloadConfigFromFile() {
        ConfigUtil.reloadFromFile();
        System.out.println("Configuración recargada desde archivo a RAM");
    }
}