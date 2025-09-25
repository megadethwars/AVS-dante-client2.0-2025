package com.example.DanteClient.data.util;

import com.example.DanteClient.data.singleton.ConfigSingleton;
import com.example.DanteClient.data.model.DanteConfig;
import com.example.DanteClient.data.model.Channel;

import java.util.List;

/**
 * Clase utilitaria para acceso estático súper fácil a la configuración desde RAM
 * Usar desde cualquier clase sin inyección de dependencias
 */
public class ConfigUtil {
    
    /**
     * Obtiene la configuración completa desde RAM
     */
    public static DanteConfig getConfig() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getConfig() : null;
    }
    
    /**
     * Obtiene el servidor desde RAM
     */
    public static String getServer() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getServer() : "192.168.1.100";
    }
    
    /**
     * Obtiene el puerto desde RAM
     */
    public static String getPort() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getPort() : "8080";
    }
    
    /**
     * Obtiene la dirección multicast desde RAM
     */
    public static String getMulticastAddress() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getMulticastAddress() : "224.0.0.1";
    }
    
    /**
     * Obtiene el puerto multicast desde RAM
     */
    public static String getMulticastPort() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getMulticastPort() : "5000";
    }
    
    /**
     * Obtiene el chunk size desde RAM
     */
    public static String getChunkSize() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getChunkSize() : "1024";
    }
    
    /**
     * Obtiene el timeout desde RAM
     */
    public static String getTimeout() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getTimeout() : "5000";
    }
    
    /**
     * Obtiene el número de canales desde RAM
     */
    public static int getChannelNumbers() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getChannelNumbers() : 64;
    }
    
    /**
     * Obtiene la frecuencia desde RAM
     */
    public static int getFrequency() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getFrequency() : 44100;
    }
    
    /**
     * Obtiene todos los canales desde RAM
     */
    public static List<Channel> getAllChannels() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getAllChannels() : java.util.Collections.emptyList();
    }
    
    /**
     * Obtiene un canal específico por ID desde RAM
     */
    public static Channel getChannelById(int id) {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getChannelById(id) : null;
    }
    
    /**
     * Obtiene solo los canales habilitados desde RAM
     */
    public static List<Channel> getEnabledChannels() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getEnabledChannels() : java.util.Collections.emptyList();
    }
    
    /**
     * Verifica si un canal está habilitado desde RAM
     */
    public static boolean isChannelEnabled(int id) {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null && singleton.isChannelEnabled(id);
    }
    
    /**
     * Obtiene el nombre de un canal desde RAM
     */
    public static String getChannelName(int id) {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getChannelName(id) : null;
    }
    
    /**
     * Cuenta el total de canales desde RAM
     */
    public static int getTotalChannelsCount() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getTotalChannelsCount() : 0;
    }
    
    /**
     * Cuenta los canales habilitados desde RAM
     */
    public static int getEnabledChannelsCount() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getEnabledChannelsCount() : 0;
    }
    
    /**
     * Obtiene información de estado desde RAM
     */
    public static String getConfigStatus() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        return singleton != null ? singleton.getConfigStatus() : "Singleton no disponible";
    }
    
    /**
     * Actualiza la configuración en RAM (llamar después de guardar archivo)
     */
    public static void updateConfigInMemory(DanteConfig newConfig) {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        if (singleton != null) {
            singleton.updateConfigInMemory(newConfig);
        }
    }
    
    /**
     * Recarga la configuración desde archivo a RAM
     */
    public static void reloadFromFile() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        if (singleton != null) {
            singleton.reloadFromFile();
        }
    }
    
    /**
     * Imprime la configuración actual (para debugging)
     */
    public static void printConfig() {
        ConfigSingleton singleton = ConfigSingleton.getInstance();
        if (singleton != null) {
            singleton.printConfig();
        } else {
            System.out.println("ConfigSingleton no disponible");
        }
    }
}