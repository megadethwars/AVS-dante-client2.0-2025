package com.example.DanteClient.thread.model;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Modelo que representa un thread de procesamiento de canal
 */
public class ChannelThread {
    
    private final int channelId;
    private final String channelName;
    private final LocalDateTime startTime;
    private final AtomicBoolean running;
    private CompletableFuture<Void> future; // No final para poder asignar después
    private volatile String status;
    private volatile String currentTask;
    private volatile int volume; // Volumen del canal (0-100)
    private ThreadEventListener eventListener; // Listener para notificaciones
    
    public ChannelThread(int channelId, String channelName, CompletableFuture<Void> future) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.startTime = LocalDateTime.now();
        this.running = new AtomicBoolean(true);
        this.future = future;
        this.status = "RUNNING";
        this.currentTask = "Inicializando...";
        this.volume = 0; // Volumen inicial por defecto
    }
    
    /**
     * Constructor alternativo sin future (se puede asignar después)
     */
    public ChannelThread(int channelId, String channelName) {
        this(channelId, channelName, null);
    }
    
    /**
     * Detiene el thread de forma segura
     */
    public void stop() {
        running.set(false);
        status = "STOPPING";
        currentTask = "Deteniendo thread...";
        
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
        
        status = "STOPPED";
        currentTask = "Thread detenido";
    }
    
    /**
     * Verifica si el thread está ejecutándose
     */
    public boolean isRunning() {
        return running.get() && !future.isDone();
    }
    
    /**
     * Actualiza la tarea actual que está ejecutando el thread
     */
    public void updateCurrentTask(String task) {
        this.currentTask = task;
    }
    
    /**
     * Actualiza el estado del thread
     */
    public void updateStatus(String newStatus) {
        String oldStatus = this.status;
        this.status = newStatus;
        
        // Notificar cambio de estado si hay listener
        if (eventListener != null && !oldStatus.equals(newStatus)) {
            eventListener.onThreadStatusChanged(channelId, channelName, oldStatus, newStatus);
        }
    }
    
    /**
     * Asigna el CompletableFuture al thread
     */
    public void setFuture(CompletableFuture<Void> future) {
        this.future = future;
    }
    
    /**
     * Establece el volumen del canal
     */
    public void setVolume(int volume) {
        if (volume < 0 || volume > 100) {
            throw new IllegalArgumentException("El volumen debe estar entre 0 y 100");
        }
        
        int oldVolume = this.volume;
        this.volume = volume;
        
        System.out.println("🔊 Canal " + channelId + " - Volumen cambiado de " + oldVolume + " a " + volume);
        updateCurrentTask("Volumen ajustado a " + volume + "%");
    }
    
    /**
     * Obtiene el volumen actual del canal
     */
    public int getVolume() {
        return volume;
    }
    
    /**
     * Establece el listener para eventos del thread
     */
    public void setEventListener(ThreadEventListener eventListener) {
        this.eventListener = eventListener;
    }
    
    /**
     * Método run que ejecuta el bucle principal del thread
     */
    public void run() {
        try {
            System.out.println("🚀 Iniciando thread para canal " + channelId + " (" + channelName + ")");
            updateStatus("RUNNING");
            updateCurrentTask("Ejecutando bucle principal");
            
            while (running.get()) {
                System.out.println("estoy corriendo - Canal " + channelId + " (" + channelName + ") - Volumen: " + volume + "%");
                
                // Simular procesamiento de audio con el volumen actual
                if (volume > 0) {
                    updateCurrentTask("Procesando audio - Vol: " + volume + "%");
                } else {
                    updateCurrentTask("Silenciado - Vol: 0%");
                }
                
                // Esperar 1 segundo
                Thread.sleep(1000);
            }
            
            updateStatus("FINISHED");
            updateCurrentTask("Bucle terminado");
            System.out.println("✅ Thread terminado para canal " + channelId + " (" + channelName + ")");
            
            // Notificar finalización normal
            if (eventListener != null) {
                eventListener.onThreadFinished(channelId, channelName, "Normal completion");
            }
            
        } catch (InterruptedException e) {
            updateStatus("INTERRUPTED");
            updateCurrentTask("Thread interrumpido");
            System.out.println("⚠️ Thread interrumpido para canal " + channelId + " (" + channelName + ")");
            
            // Notificar interrupción
            if (eventListener != null) {
                eventListener.onThreadFinished(channelId, channelName, "Thread interrupted");
            }
            
        } catch (Exception e) {
            updateStatus("ERROR");
            updateCurrentTask("Error: " + e.getMessage());
            System.err.println("❌ Error en thread canal " + channelId + ": " + e.getMessage());
            
            // Notificar excepción
            if (eventListener != null) {
                eventListener.onThreadException(channelId, channelName, e.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
    
    // Getters
    public int getChannelId() {
        return channelId;
    }
    
    public String getChannelName() {
        return channelName;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public AtomicBoolean getRunningFlag() {
        return running;
    }
    
    public CompletableFuture<Void> getFuture() {
        return future;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getCurrentTask() {
        return currentTask;
    }
    
    /**
     * Obtiene información resumida del thread
     */
    public String getThreadInfo() {
        return String.format(
            "Thread Canal %d (%s) - Estado: %s - Tarea: %s - Inicio: %s",
            channelId, channelName, status, currentTask, startTime
        );
    }
    
    @Override
    public String toString() {
        return "ChannelThread{" +
                "channelId=" + channelId +
                ", channelName='" + channelName + '\'' +
                ", status='" + status + '\'' +
                ", currentTask='" + currentTask + '\'' +
                ", volume=" + volume +
                ", startTime=" + startTime +
                ", running=" + running.get() +
                '}';
    }
}