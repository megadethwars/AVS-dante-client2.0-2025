package com.example.DanteClient.server.service;

import com.example.DanteClient.server.model.ServerStatus;
import com.example.DanteClient.data.util.ConfigUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;

/**
 * Servicio para verificar el estado de conectividad del servidor remoto
 */
@Service
public class ServerPingService {
    
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    
    /**
     * Realiza ping al servidor configurado en el singleton
     */
    public ServerStatus pingConfiguredServer() {
        String serverAddress = ConfigUtil.getServer();
        String port = ConfigUtil.getPort();
        
        System.out.println("🔍 Realizando ping al servidor configurado: " + serverAddress + ":" + port);
        
        return pingServer(serverAddress, port);
    }
    
    /**
     * Realiza ping a un servidor específico
     */
    public ServerStatus pingServer(String serverAddress, String port) {
        ServerStatus status = new ServerStatus(serverAddress, port);

        try {
            // Obtener timeout desde configuración
            String timeoutConfig = ConfigUtil.getTimeout();
            int timeoutMs = timeoutConfig != null ? Integer.parseInt(timeoutConfig) : DEFAULT_TIMEOUT_MS;
            status.setTimeoutMs(timeoutMs);

            // Realizar solo ping ICMP a la dirección (no al puerto)
            boolean icmpReachable = pingICMP(serverAddress, timeoutMs);

            long responseTime = icmpReachable ? timeoutMs : -1;
            status.setReachable(icmpReachable);
            status.setResponseTimeMs(responseTime);

            if (icmpReachable) {
                status.setStatus("ONLINE");
                status.setErrorMessage(null);
                System.out.println("✅ Servidor " + serverAddress + " está ONLINE (ICMP)");
            } else {
                status.setStatus("UNREACHABLE");
                status.setErrorMessage("No se pudo alcanzar el servidor por ICMP");
                System.out.println("❌ Servidor " + serverAddress + " no alcanzable por ICMP");
            }

        } catch (Exception e) {
            status.setReachable(false);
            status.setStatus("ERROR");
            status.setErrorMessage("Error durante ping: " + e.getMessage());
            status.setResponseTimeMs(-1);
            System.err.println("❌ Error al hacer ping a " + serverAddress + ": " + e.getMessage());
        }

        return status;
    }
    
    /**
     * Realiza ping ICMP tradicional
     */
    private boolean pingICMP(String serverAddress, int timeoutMs) {
        try {
            InetAddress address = InetAddress.getByName(serverAddress);
            
            System.out.println("🏓 Realizando ICMP ping a " + address.getHostAddress());
            
            long startTime = System.currentTimeMillis();
            boolean reachable = address.isReachable(timeoutMs);
            long pingTime = System.currentTimeMillis() - startTime;
            
            if (reachable) {
                System.out.println("✅ ICMP ping exitoso en " + pingTime + "ms");
            } else {
                System.out.println("❌ ICMP ping falló (timeout: " + timeoutMs + "ms)");
            }
            
            return reachable;
            
        } catch (IOException e) {
            System.err.println("❌ Error en ICMP ping: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Realiza ping TCP a un puerto específico
     */
    private ServerStatus pingTCPPort(String serverAddress, String port, int timeoutMs) {
        ServerStatus status = new ServerStatus(serverAddress, port);
        
        try {
            long startTime = System.currentTimeMillis();
            boolean portOpen = isPortOpen(serverAddress, port, timeoutMs);
            long responseTime = System.currentTimeMillis() - startTime;
            
            status.setReachable(portOpen);
            status.setResponseTimeMs(responseTime);
            status.setTimeoutMs(timeoutMs);
            
            if (portOpen) {
                status.setStatus("TCP_REACHABLE");
                status.setErrorMessage(null);
                System.out.println("✅ Puerto TCP " + port + " abierto en " + serverAddress + " (" + responseTime + "ms)");
            } else {
                status.setStatus("UNREACHABLE");
                status.setErrorMessage("No se pudo conectar al puerto " + port + " en timeout de " + timeoutMs + "ms");
                System.out.println("❌ Puerto TCP " + port + " no alcanzable en " + serverAddress);
            }
            
        } catch (Exception e) {
            status.setReachable(false);
            status.setStatus("ERROR");
            status.setErrorMessage("Error en TCP ping: " + e.getMessage());
            status.setResponseTimeMs(-1);
            System.err.println("❌ Error en TCP ping: " + e.getMessage());
        }
        
        return status;
    }
    
    /**
     * Verifica si un puerto está abierto usando Socket
     */
    private boolean isPortOpen(String serverAddress, String port, int timeoutMs) {
        try {
            int portNum = Integer.parseInt(port);
            
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(serverAddress, portNum);
            
            socket.connect(socketAddress, timeoutMs);
            socket.close();
            
            return true;
            
        } catch (IOException | NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Realiza múltiples pings para obtener estadísticas
     */
    public ServerStatus pingMultiple(int count) {
        String serverAddress = ConfigUtil.getServer();
        String port = ConfigUtil.getPort();
        
        System.out.println("📊 Realizando " + count + " pings a " + serverAddress + ":" + port);
        
        long totalResponseTime = 0;
        int successfulPings = 0;
        
        for (int i = 0; i < count; i++) {
            ServerStatus singlePing = pingServer(serverAddress, port);
            
            if (singlePing.isReachable()) {
                successfulPings++;
                totalResponseTime += singlePing.getResponseTimeMs();
            }
            
            // Pequeña pausa entre pings
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Crear resultado consolidado
        ServerStatus result = new ServerStatus(serverAddress, port);
        
        if (successfulPings > 0) {
            result.setReachable(true);
            result.setResponseTimeMs(totalResponseTime / successfulPings); // Promedio
            result.setStatus("TESTED_" + successfulPings + "_OF_" + count);
            result.setErrorMessage(null);
        } else {
            result.setReachable(false);
            result.setStatus("ALL_FAILED");
            result.setErrorMessage("Todos los " + count + " pings fallaron");
            result.setResponseTimeMs(-1);
        }
        
        System.out.println("📈 Resultado: " + successfulPings + "/" + count + " pings exitosos");
        
        return result;
    }
    
    /**
     * Obtiene información detallada del servidor
     */
    public ServerStatus getDetailedServerInfo() {
        ServerStatus status = pingConfiguredServer();
        
        // Agregar información adicional
        status.setLastChecked(LocalDateTime.now());
        
        // Información de configuración actual
        String configInfo = String.format(
            "Server: %s:%s, Timeout: %s, Multicast: %s:%s",
            ConfigUtil.getServer(),
            ConfigUtil.getPort(),
            ConfigUtil.getTimeout(),
            ConfigUtil.getMulticastAddress(),
            ConfigUtil.getMulticastPort()
        );
        
        System.out.println("ℹ️ Config actual: " + configInfo);
        
        return status;
    }

    public void turnOFFSystem(){
        System.out.println("Apagando sistema...");
        System.exit(0);
    }
}