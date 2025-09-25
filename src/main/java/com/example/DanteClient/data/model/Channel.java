package com.example.DanteClient.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Modelo que representa un canal de audio en la configuración de Dante
 */
public class Channel {
    
    @JsonProperty("id")
    private int id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("enabled")
    private boolean enabled;
    
    // Constructor por defecto
    public Channel() {}
    
    // Constructor con parámetros
    public Channel(int id, String name, boolean enabled) {
        this.id = id;
        this.name = name;
        this.enabled = enabled;
    }
    
    // Constructor sin id (para generar automáticamente)
    public Channel(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }
    
    // Getters y Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return "Channel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", enabled=" + enabled +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Channel channel = (Channel) o;
        return id == channel.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}