package com.example.DanteClient.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

/**
 * Modelo que representa la configuración completa de Dante
 */
public class DanteConfig {
    
    @JsonProperty("server")
    private String server;
    
    @JsonProperty("port")
    private String port;
    
    @JsonProperty("multicast_address")
    private String multicastAddress;
    
    @JsonProperty("multicast_port")
    private String multicastPort;
    
    @JsonProperty("chunk_size")
    private String chunkSize;
    
    @JsonProperty("timeout")
    private String timeout;
    
    @JsonProperty("channel_numbers")
    private int channelNumbers;
    
    @JsonProperty("frequency")
    private int frequency;
    
    @JsonProperty("channels")
    private List<Channel> channels;
    
    // Constructor por defecto
    public DanteConfig() {
        this.channels = new ArrayList<>();
    }
    
    // Constructor con parámetros básicos
    public DanteConfig(String server, String port, String multicastAddress, 
                      String multicastPort, String chunkSize, String timeout,
                      int channelNumbers, int frequency) {
        this.server = server;
        this.port = port;
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.chunkSize = chunkSize;
        this.timeout = timeout;
        this.channelNumbers = channelNumbers;
        this.frequency = frequency;
        this.channels = new ArrayList<>();
    }
    
    // Getters y Setters
    public String getServer() {
        return server;
    }
    
    public void setServer(String server) {
        this.server = server;
    }
    
    public String getPort() {
        return port;
    }
    
    public void setPort(String port) {
        this.port = port;
    }
    
    public String getMulticastAddress() {
        return multicastAddress;
    }
    
    public void setMulticastAddress(String multicastAddress) {
        this.multicastAddress = multicastAddress;
    }
    
    public String getMulticastPort() {
        return multicastPort;
    }
    
    public void setMulticastPort(String multicastPort) {
        this.multicastPort = multicastPort;
    }
    
    public String getChunkSize() {
        return chunkSize;
    }
    
    public void setChunkSize(String chunkSize) {
        this.chunkSize = chunkSize;
    }
    
    public String getTimeout() {
        return timeout;
    }
    
    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }
    
    public int getChannelNumbers() {
        return channelNumbers;
    }
    
    public void setChannelNumbers(int channelNumbers) {
        this.channelNumbers = channelNumbers;
    }
    
    public int getFrequency() {
        return frequency;
    }
    
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
    
    public List<Channel> getChannels() {
        return channels;
    }
    
    public void setChannels(List<Channel> channels) {
        this.channels = channels != null ? channels : new ArrayList<>();
    }
    
    // Métodos auxiliares para manejo de canales
    public void addChannel(Channel channel) {
        if (this.channels == null) {
            this.channels = new ArrayList<>();
        }
        // Asignar ID automáticamente si no tiene uno
        if (channel.getId() <= 0) {
            channel.setId(getNextChannelId());
        }
        this.channels.add(channel);
    }
    
    public void removeChannel(int index) {
        if (this.channels != null && index >= 0 && index < this.channels.size()) {
            this.channels.remove(index);
        }
    }
    
    public Channel getChannel(int index) {
        if (this.channels != null && index >= 0 && index < this.channels.size()) {
            return this.channels.get(index);
        }
        return null;
    }
    
    // Métodos nuevos para trabajar con IDs
    public Channel getChannelById(int id) {
        if (this.channels != null) {
            return this.channels.stream()
                    .filter(channel -> channel.getId() == id)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
    
    public boolean removeChannelById(int id) {
        if (this.channels != null) {
            return this.channels.removeIf(channel -> channel.getId() == id);
        }
        return false;
    }
    
    public boolean updateChannelById(int id, String name, Boolean enabled) {
        Channel channel = getChannelById(id);
        if (channel != null) {
            if (name != null) {
                channel.setName(name);
            }
            if (enabled != null) {
                channel.setEnabled(enabled);
            }
            return true;
        }
        return false;
    }
    
    private int getNextChannelId() {
        if (this.channels == null || this.channels.isEmpty()) {
            return 1;
        }
        return this.channels.stream()
                .mapToInt(Channel::getId)
                .max()
                .orElse(0) + 1;
    }
    
    @Override
    public String toString() {
        return "DanteConfig{" +
                "server='" + server + '\'' +
                ", port='" + port + '\'' +
                ", multicastAddress='" + multicastAddress + '\'' +
                ", multicastPort='" + multicastPort + '\'' +
                ", chunkSize='" + chunkSize + '\'' +
                ", timeout='" + timeout + '\'' +
                ", channelNumbers=" + channelNumbers +
                ", frequency=" + frequency +
                ", channels=" + channels +
                '}';
    }
}