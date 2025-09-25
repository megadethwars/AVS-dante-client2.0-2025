package com.example.DanteClient.thread.controller;

import com.example.DanteClient.thread.service.ChannelThreadService;
import com.example.DanteClient.thread.model.ChannelThread;
import com.example.DanteClient.data.util.ConfigUtil;
import com.example.DanteClient.data.model.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador REST para gestionar threads de canales de audio
 */
@RestController
@RequestMapping("/api/threads")
@CrossOrigin(origins = "*")
public class ChannelThreadController {
    
    @Autowired
    private ChannelThreadService threadService;
    
    /**
     * Crea un nuevo thread para un canal específico
     * POST /api/threads/channel/{channelId}
     */
    @PostMapping("/channel/{channelId}")
    public ResponseEntity<?> startChannelThread(@PathVariable int channelId) {
        // El service lanzará excepciones que serán manejadas por ThreadExceptionHandler
        threadService.startChannelThread(channelId);
        
        // Si llegamos aquí, el thread se creó exitosamente
        Channel channel = ConfigUtil.getChannelById(channelId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Thread creado exitosamente para canal " + channelId);
        response.put("channelId", channelId);
        response.put("channelName", channel.getName());
        response.put("startTime", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Detiene un thread específico por ID de canal
     * DELETE /api/threads/channel/{channelId}
     */
    @DeleteMapping("/channel/{channelId}")
    public ResponseEntity<?> stopChannelThread(@PathVariable int channelId) {
        // El service lanzará excepciones que serán manejadas por ThreadExceptionHandler
        threadService.stopChannelThread(channelId);
        
        // Si llegamos aquí, el thread se detuvo exitosamente
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Thread detenido exitosamente para canal " + channelId);
        response.put("channelId", channelId);
        response.put("stopTime", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene información de un thread específico
     * GET /api/threads/channel/{channelId}
     */
    @GetMapping("/channel/{channelId}")
    public ResponseEntity<?> getThreadInfo(@PathVariable int channelId) {
        ChannelThread thread = threadService.getThreadInfo(channelId);
        
        if (thread != null) {
            Map<String, Object> info = new HashMap<>();
            info.put("channelId", thread.getChannelId());
            info.put("channelName", thread.getChannelName());
            info.put("status", thread.getStatus());
            info.put("currentTask", thread.getCurrentTask());
            info.put("startTime", thread.getStartTime());
            info.put("isRunning", thread.isRunning());
            
            return ResponseEntity.ok(info);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Obtiene lista de todos los threads activos
     * GET /api/threads
     */
    @GetMapping
    public ResponseEntity<?> getAllActiveThreads() {
        List<ChannelThread> activeThreads = threadService.getAllActiveThreads();
        
        Map<String, Object> response = new HashMap<>();
        response.put("activeThreadsCount", activeThreads.size());
        response.put("threads", activeThreads.stream().map(thread -> {
            Map<String, Object> threadInfo = new HashMap<>();
            threadInfo.put("channelId", thread.getChannelId());
            threadInfo.put("channelName", thread.getChannelName());
            threadInfo.put("status", thread.getStatus());
            threadInfo.put("currentTask", thread.getCurrentTask());
            threadInfo.put("startTime", thread.getStartTime());
            threadInfo.put("isRunning", thread.isRunning());
            return threadInfo;
        }).toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verifica si un canal tiene un thread activo
     * GET /api/threads/channel/{channelId}/status
     */
    @GetMapping("/channel/{channelId}/status")
    public ResponseEntity<?> checkThreadStatus(@PathVariable int channelId) {
        boolean isActive = threadService.isThreadActive(channelId);
        ChannelThread thread = threadService.getThreadInfo(channelId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("channelId", channelId);
        response.put("hasActiveThread", isActive);
        
        if (thread != null) {
            response.put("status", thread.getStatus());
            response.put("currentTask", thread.getCurrentTask());
            response.put("startTime", thread.getStartTime());
        } else {
            response.put("status", "NO_THREAD");
            response.put("currentTask", null);
            response.put("startTime", null);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Detiene todos los threads activos
     * DELETE /api/threads/all
     */
    @DeleteMapping("/all")
    public ResponseEntity<?> stopAllThreads() {
        int stoppedCount = threadService.stopAllThreads();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Detenidos " + stoppedCount + " threads");
        response.put("stoppedCount", stoppedCount);
        response.put("stopTime", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Crea threads para todos los canales habilitados
     * POST /api/threads/all-enabled
     */
    @PostMapping("/all-enabled")
    public ResponseEntity<?> startAllEnabledChannelThreads() {
        List<Channel> enabledChannels = ConfigUtil.getEnabledChannels();
        
        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        
        for (Channel channel : enabledChannels) {
            Map<String, Object> result = new HashMap<>();
            result.put("channelId", channel.getId());
            result.put("channelName", channel.getName());
            
            boolean success = threadService.startChannelThread(channel.getId());
            if (success) {
                successCount++;
                result.put("status", "SUCCESS");
                result.put("message", "Thread creado exitosamente");
            } else {
                failCount++;
                result.put("status", "FAILED");
                result.put("message", "Error al crear thread (puede que ya exista)");
            }
            
            results.add(result);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalChannels", enabledChannels.size());
        response.put("successCount", successCount);
        response.put("failCount", failCount);
        response.put("results", results);
        response.put("startTime", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene estadísticas generales de threads
     * GET /api/threads/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getThreadStats() {
        int activeThreadsCount = threadService.getActiveThreadsCount();
        int totalChannels = ConfigUtil.getTotalChannelsCount();
        int enabledChannels = ConfigUtil.getEnabledChannelsCount();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeThreads", activeThreadsCount);
        stats.put("totalChannels", totalChannels);
        stats.put("enabledChannels", enabledChannels);
        stats.put("threadsPercentage", enabledChannels > 0 ? (double) activeThreadsCount / enabledChannels * 100 : 0);
        stats.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(stats);
    }
}