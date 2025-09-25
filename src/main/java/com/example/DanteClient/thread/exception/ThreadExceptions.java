package com.example.DanteClient.thread.exception;

/**
 * Excepciones específicas para threads de canales
 */
public class ThreadExceptions {
    
    /**
     * Error 1: El thread ya existe para el canal
     */
    public static class ThreadAlreadyExistsException extends ThreadException {
        public ThreadAlreadyExistsException(int channelId) {
            super(1, "THREAD_ALREADY_EXISTS", 
                  "Ya existe un thread activo para el canal " + channelId);
        }
    }
    
    /**
     * Error 2: No se puede crear el thread
     */
    public static class ThreadCreationException extends ThreadException {
        public ThreadCreationException(int channelId, String reason) {
            super(2, "THREAD_CREATION_FAILED", 
                  "No se puede crear thread para canal " + channelId + ": " + reason);
        }
        
        public ThreadCreationException(int channelId, Throwable cause) {
            super(2, "THREAD_CREATION_FAILED", 
                  "Error interno al crear thread para canal " + channelId, cause);
        }
    }
    
    /**
     * Error 3: Canal no encontrado
     */
    public static class ChannelNotFoundException extends ThreadException {
        public ChannelNotFoundException(int channelId) {
            super(3, "CHANNEL_NOT_FOUND", 
                  "Canal con ID " + channelId + " no encontrado en la configuración");
        }
    }
    
    /**
     * Error 4: Canal deshabilitado
     */
    public static class ChannelDisabledException extends ThreadException {
        public ChannelDisabledException(int channelId) {
            super(4, "CHANNEL_DISABLED", 
                  "No se puede crear thread para canal deshabilitado: " + channelId);
        }
    }
    
    /**
     * Error 5: Thread no encontrado para detener
     */
    public static class ThreadNotFoundException extends ThreadException {
        public ThreadNotFoundException(int channelId) {
            super(5, "THREAD_NOT_FOUND", 
                  "No existe thread activo para el canal " + channelId);
        }
    }
    
    /**
     * Error 6: Error al detener thread
     */
    public static class ThreadStopException extends ThreadException {
        public ThreadStopException(int channelId, String reason) {
            super(6, "THREAD_STOP_FAILED", 
                  "Error al detener thread del canal " + channelId + ": " + reason);
        }
        
        public ThreadStopException(int channelId, Throwable cause) {
            super(6, "THREAD_STOP_FAILED", 
                  "Error interno al detener thread del canal " + channelId, cause);
        }
    }
    
    /**
     * Error 7: Pool de threads lleno
     */
    public static class ThreadPoolFullException extends ThreadException {
        public ThreadPoolFullException() {
            super(7, "THREAD_POOL_FULL", 
                  "El pool de threads está lleno. No se pueden crear más threads");
        }
    }
    
    /**
     * Error 8: Thread en estado inválido
     */
    public static class InvalidThreadStateException extends ThreadException {
        public InvalidThreadStateException(int channelId, String currentState) {
            super(8, "INVALID_THREAD_STATE", 
                  "Thread del canal " + channelId + " está en estado inválido: " + currentState);
        }
    }
    
    /**
     * Error 9: Configuración inválida
     */
    public static class InvalidConfigurationException extends ThreadException {
        public InvalidConfigurationException(String reason) {
            super(9, "INVALID_CONFIGURATION", 
                  "Configuración inválida: " + reason);
        }
    }
    
    /**
     * Error 10: Límite de threads excedido
     */
    public static class ThreadLimitExceededException extends ThreadException {
        public ThreadLimitExceededException(int maxThreads) {
            super(10, "THREAD_LIMIT_EXCEEDED", 
                  "Se ha excedido el límite máximo de threads activos (" + maxThreads + ")");
        }
    }
}