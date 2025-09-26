package com.example.DanteClient.thread.model;

import java.time.LocalDateTime;

/**
 * Modelo para representar el volumen de un canal
 */
public class ChannelVolume {
    
    private int channelId;
    private int volumeLevel; // Nivel de 0 a 100
    private LocalDateTime timestamp;
    private String channelName;
    
    public ChannelVolume() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ChannelVolume(int channelId, int volumeLevel) {
        this.channelId = channelId;
        this.volumeLevel = validateVolumeLevel(volumeLevel);
        this.timestamp = LocalDateTime.now();
    }
    
    public ChannelVolume(int channelId, int volumeLevel, String channelName) {
        this.channelId = channelId;
        this.volumeLevel = validateVolumeLevel(volumeLevel);
        this.channelName = channelName;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Valida que el nivel de volumen esté entre 0 y 100
     */
    private int validateVolumeLevel(int volume) {
        if (volume < 0) return 0;
        if (volume > 100) return 100;
        return volume;
    }
    
    // Getters y Setters
    public int getChannelId() {
        return channelId;
    }
    
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }
    
    public int getVolumeLevel() {
        return volumeLevel;
    }
    
    public void setVolumeLevel(int volumeLevel) {
        this.volumeLevel = validateVolumeLevel(volumeLevel);
        this.timestamp = LocalDateTime.now(); // Actualizar timestamp al cambiar volumen
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getChannelName() {
        return channelName;
    }
    
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
    
    /**
     * Convierte a porcentaje (0.0 - 1.0)
     */
    public double getVolumeAsPercentage() {
        return volumeLevel / 100.0;
    }
    
    /**
     * Obtiene descripción del nivel de volumen
     */
    public String getVolumeDescription() {
        if (volumeLevel == 0) return "Mute";
        if (volumeLevel <= 20) return "Muy Bajo";
        if (volumeLevel <= 40) return "Bajo";
        if (volumeLevel <= 60) return "Medio";
        if (volumeLevel <= 80) return "Alto";
        return "Muy Alto";
    }
    
    /**
     * Verifica si el canal está en mute
     */
    public boolean isMuted() {
        return volumeLevel == 0;
    }
    
    @Override
    public String toString() {
        return "ChannelVolume{" +
                "channelId=" + channelId +
                ", volumeLevel=" + volumeLevel +
                ", channelName='" + channelName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChannelVolume that = (ChannelVolume) obj;
        return channelId == that.channelId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(channelId);
    }
}