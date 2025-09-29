package com.example.DanteClient.thread.service;

import com.example.DanteClient.thread.model.ChannelThread;
import com.example.DanteClient.thread.model.ThreadEventListener;
import com.example.DanteClient.data.util.ConfigUtil;
import com.example.DanteClient.data.model.Channel;
import com.example.DanteClient.thread.exception.ThreadException;
import com.example.DanteClient.thread.exception.ThreadExceptions;
import com.example.DanteClient.thread.config.ThreadWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

/**
 * Servicio para gestionar threads de canales de audio
 */
@Service
public class ChannelThreadService implements ThreadEventListener {
    
    @Autowired
    private ThreadWebSocketHandler threadWebSocketHandler;
    
    private final ConcurrentHashMap<Integer, ChannelThread> activeThreads;
    private final ScheduledExecutorService executorService;
    
    public ChannelThreadService() {
        this.activeThreads = new ConcurrentHashMap<>();
        this.executorService = Executors.newScheduledThreadPool(10);
        
        System.out.println("ChannelThreadService inicializado con pool de 10 threads");
    }
    
    /**
     * Crea y ejecuta un nuevo thread para un canal específico
     */
    public boolean startChannelThread(int channelId) {
        try {
            // Verificar si el canal existe en la configuración
            Channel channel = ConfigUtil.getChannelById(channelId);
            if (channel == null) {
                throw new ThreadExceptions.ChannelNotFoundException(channelId);
            }
            
            // Verificar si ya existe un thread para este canal
            if (activeThreads.containsKey(channelId)) {
                throw new ThreadExceptions.ThreadAlreadyExistsException(channelId);
            }
            
            // Verificar si el canal está habilitado
            if (!channel.isEnabled()) {
                throw new ThreadExceptions.ChannelDisabledException(channelId);
            }
            
            // Verificar límite de threads (máximo 20 threads simultáneos)
            if (activeThreads.size() >= 20) {
                throw new ThreadExceptions.ThreadLimitExceededException(20);
            }
            
            // Crear objeto ChannelThread primero
            ChannelThread channelThread = new ChannelThread(channelId, channel.getName());
            
            // Configurar el listener para recibir notificaciones del thread
            channelThread.setEventListener(this);
            
            // Crear y ejecutar el thread usando el método run del ChannelThread
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                channelThread.run();
            }, executorService);
            
            // Asignar el future al ChannelThread
            channelThread.setFuture(future);
            
            // Almacenar el ChannelThread
            activeThreads.put(channelId, channelThread);
            
            // Notificar via WebSocket que el thread se inició
            if (threadWebSocketHandler != null) {
                threadWebSocketHandler.notifyThreadStarted(channelId, channel.getName());
            }
            
            System.out.println("Thread iniciado para canal " + channelId + " (" + channel.getName() + ")");
            return true;
            
        } catch (ThreadException ex) {
            // Notificar excepción via WebSocket
            if (threadWebSocketHandler != null) {
                Channel channel = ConfigUtil.getChannelById(channelId);
                String channelName = (channel != null) ? channel.getName() : "Unknown";
                threadWebSocketHandler.notifyThreadException(channelId, channelName, ex.getClass().getSimpleName(), ex.getMessage());
            }
            // Re-lanzar excepciones de thread para que sean capturadas por el handler
            throw ex;
        } catch (Exception ex) {
            // Notificar excepción via WebSocket
            if (threadWebSocketHandler != null) {
                Channel channel = ConfigUtil.getChannelById(channelId);
                String channelName = (channel != null) ? channel.getName() : "Unknown";
                threadWebSocketHandler.notifyThreadException(channelId, channelName, ex.getClass().getSimpleName(), ex.getMessage());
            }
            // Convertir cualquier otra excepción en ThreadCreationException
            throw new ThreadExceptions.ThreadCreationException(channelId, ex);
        }
    }
    
    /**
     * Detiene un thread específico por ID de canal
     */
    public boolean stopChannelThread(int channelId) {
        try {
            ChannelThread channelThread = activeThreads.get(channelId);
            
            if (channelThread == null) {
                throw new ThreadExceptions.ThreadNotFoundException(channelId);
            }
            
            // Verificar estado del thread
            if (!channelThread.isRunning()) {
                throw new ThreadExceptions.InvalidThreadStateException(channelId, channelThread.getStatus());
            }
            
            // Detener el thread
            channelThread.stop();
            
            // Notificar via WebSocket que el thread se detuvo
            if (threadWebSocketHandler != null) {
                threadWebSocketHandler.notifyThreadFinished(channelId, channelThread.getChannelName(), "Manual stop");
            }
            
            // Remover de la lista de threads activos
            activeThreads.remove(channelId);
            
            System.out.println("Thread detenido para canal " + channelId + " (" + channelThread.getChannelName() + ")");
            return true;
            
        } catch (ThreadException ex) {
            // Notificar excepción via WebSocket
            if (threadWebSocketHandler != null) {
                ChannelThread thread = activeThreads.get(channelId);
                String channelName = (thread != null) ? thread.getChannelName() : "Unknown";
                threadWebSocketHandler.notifyThreadException(channelId, channelName, ex.getClass().getSimpleName(), ex.getMessage());
            }
            // Re-lanzar excepciones de thread para que sean capturadas por el handler
            throw ex;
        } catch (Exception ex) {
            // Notificar excepción via WebSocket
            if (threadWebSocketHandler != null) {
                ChannelThread thread = activeThreads.get(channelId);
                String channelName = (thread != null) ? thread.getChannelName() : "Unknown";
                threadWebSocketHandler.notifyThreadException(channelId, channelName, ex.getClass().getSimpleName(), ex.getMessage());
            }
            // Convertir cualquier otra excepción en ThreadStopException
            throw new ThreadExceptions.ThreadStopException(channelId, ex);
        }
    }
    
    /**
     * Detiene todos los threads activos
     */
    public int stopAllThreads() {
        int stoppedCount = 0;
        
        for (ChannelThread channelThread : activeThreads.values()) {
            channelThread.stop();
            stoppedCount++;
        }
        
        activeThreads.clear();
        System.out.println("Detenidos " + stoppedCount + " threads activos");
        return stoppedCount;
    }
    
    /**
     * Obtiene información de un thread específico
     */
    public ChannelThread getThreadInfo(int channelId) {
        return activeThreads.get(channelId);
    }
    
    /**
     * Obtiene un thread específico por ID de canal
     */
    public ChannelThread getChannelThreadById(int channelId) {
        return activeThreads.get(channelId);
    }
    
    /**
     * Obtiene la lista de todos los threads activos
     */
    public List<ChannelThread> getAllActiveThreads() {
        return new ArrayList<>(activeThreads.values());
    }
    
    /**
     * Verifica si existe un thread activo para un canal
     */
    public boolean isThreadActive(int channelId) {
        ChannelThread thread = activeThreads.get(channelId);
        return thread != null && thread.isRunning();
    }
    
    /**
     * Obtiene el número de threads activos
     */
    public int getActiveThreadsCount() {
        return activeThreads.size();
    }
    

    
    /**
     * Método de limpieza para cerrar el servicio
     */
    public void shutdown() {
        System.out.println("Cerrando ChannelThreadService...");
        stopAllThreads();
        executorService.shutdown();
        
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        
        System.out.println("ChannelThreadService cerrado");
    }
    
    // Implementación de ThreadEventListener
    
    @Override
    public void onThreadFinished(int channelId, String channelName, String reason) {
        System.out.println("🏁 Thread finalizado - Canal: " + channelId + ", Razón: " + reason);
        
        // Remover thread de la lista activa
        activeThreads.remove(channelId);
        
        // Notificar via WebSocket
        if (threadWebSocketHandler != null) {
            threadWebSocketHandler.notifyThreadFinished(channelId, channelName, reason);
        }
    }
    
    @Override
    public void onThreadException(int channelId, String channelName, String exceptionType, String errorMessage) {
        System.err.println("❌ Excepción en thread - Canal: " + channelId + ", Tipo: " + exceptionType + ", Error: " + errorMessage);
        
        // Remover thread de la lista activa si hay error crítico
        if (exceptionType.contains("Error") || exceptionType.contains("Exception")) {
            activeThreads.remove(channelId);
        }
        
        // Notificar via WebSocket
        if (threadWebSocketHandler != null) {
            threadWebSocketHandler.notifyThreadException(channelId, channelName, exceptionType, errorMessage);
        }
    }
    
    @Override
    public void onThreadStatusChanged(int channelId, String channelName, String oldStatus, String newStatus) {
        System.out.println("🔄 Cambio de estado - Canal: " + channelId + ", " + oldStatus + " -> " + newStatus);
        
        // Notificar via WebSocket
        if (threadWebSocketHandler != null) {
            threadWebSocketHandler.notifyThreadStatusChange(channelId, channelName, oldStatus, newStatus);
        }
    }
}