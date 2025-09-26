package com.example.DanteClient.data.exception;

/**
 * Excepciones específicas para operaciones de configuración
 */
public class ConfigExceptions {
    
    /**
     * Error 101: Archivo de configuración no encontrado
     */
    public static class ConfigFileNotFoundException extends ConfigException {
        public ConfigFileNotFoundException(String filePath) {
            super(101, "CONFIG_FILE_NOT_FOUND", 
                  "Archivo de configuración no encontrado: " + filePath);
        }
    }
    
    /**
     * Error 102: Error al leer archivo de configuración
     */
    public static class ConfigFileReadException extends ConfigException {
        public ConfigFileReadException(String filePath, Throwable cause) {
            super(102, "CONFIG_FILE_READ_ERROR", 
                  "Error al leer archivo de configuración: " + filePath, cause);
        }
    }
    
    /**
     * Error 103: Error al escribir archivo de configuración
     */
    public static class ConfigFileWriteException extends ConfigException {
        public ConfigFileWriteException(String filePath, Throwable cause) {
            super(103, "CONFIG_FILE_WRITE_ERROR", 
                  "Error al escribir archivo de configuración: " + filePath, cause);
        }
    }
    
    /**
     * Error 104: Canal no encontrado
     */
    public static class ChannelNotFoundException extends ConfigException {
        public ChannelNotFoundException(int channelId) {
            super(104, "CHANNEL_NOT_FOUND", 
                  "Canal con ID " + channelId + " no encontrado en la configuración");
        }
        
        public ChannelNotFoundException(String identifier) {
            super(104, "CHANNEL_NOT_FOUND", 
                  "Canal " + identifier + " no encontrado en la configuración");
        }
    }
    
    /**
     * Error 105: Canal ya existe
     */
    public static class ChannelAlreadyExistsException extends ConfigException {
        public ChannelAlreadyExistsException(int channelId) {
            super(105, "CHANNEL_ALREADY_EXISTS", 
                  "Ya existe un canal con ID " + channelId);
        }
    }
    
    /**
     * Error 106: Propiedad de configuración inválida
     */
    public static class InvalidConfigPropertyException extends ConfigException {
        public InvalidConfigPropertyException(String property, String value) {
            super(106, "INVALID_CONFIG_PROPERTY", 
                  "Propiedad de configuración inválida: " + property + " = " + value);
        }
        
        public InvalidConfigPropertyException(String property) {
            super(106, "INVALID_CONFIG_PROPERTY", 
                  "Propiedad de configuración no reconocida: " + property);
        }
    }
    
    /**
     * Error 107: Valor de configuración inválido
     */
    public static class InvalidConfigValueException extends ConfigException {
        public InvalidConfigValueException(String field, Object value, String expectedType) {
            super(107, "INVALID_CONFIG_VALUE", 
                  "Valor inválido para campo '" + field + "': " + value + 
                  " (se esperaba " + expectedType + ")");
        }
    }
    
    /**
     * Error 108: Configuración JSON malformada
     */
    public static class MalformedConfigException extends ConfigException {
        public MalformedConfigException(String details, Throwable cause) {
            super(108, "MALFORMED_CONFIG", 
                  "Configuración JSON malformada: " + details, cause);
        }
    }
    
    /**
     * Error 109: Límite de canales excedido
     */
    public static class ChannelLimitExceededException extends ConfigException {
        public ChannelLimitExceededException(int maxChannels) {
            super(109, "CHANNEL_LIMIT_EXCEEDED", 
                  "Se ha excedido el límite máximo de canales (" + maxChannels + ")");
        }
    }
    
    /**
     * Error 110: ID de canal inválido
     */
    public static class InvalidChannelIdException extends ConfigException {
        public InvalidChannelIdException(int channelId, String reason) {
            super(110, "INVALID_CHANNEL_ID", 
                  "ID de canal inválido " + channelId + ": " + reason);
        }
    }
    
    /**
     * Error 111: Configuración no inicializada
     */
    public static class ConfigNotInitializedException extends ConfigException {
        public ConfigNotInitializedException() {
            super(111, "CONFIG_NOT_INITIALIZED", 
                  "La configuración no ha sido inicializada correctamente");
        }
    }
    
    /**
     * Error 112: Error de sincronización con RAM
     */
    public static class ConfigSyncException extends ConfigException {
        public ConfigSyncException(String operation, Throwable cause) {
            super(112, "CONFIG_SYNC_ERROR", 
                  "Error al sincronizar configuración con RAM durante: " + operation, cause);
        }
    }
    
    /**
     * Error 113: Datos de canal inválidos
     */
    public static class InvalidChannelDataException extends ConfigException {
        public InvalidChannelDataException(String field, String reason) {
            super(113, "INVALID_CHANNEL_DATA", 
                  "Datos de canal inválidos en campo '" + field + "': " + reason);
        }
    }
    
    /**
     * Error 114: Operación no permitida
     */
    public static class ConfigOperationNotAllowedException extends ConfigException {
        public ConfigOperationNotAllowedException(String operation, String reason) {
            super(114, "CONFIG_OPERATION_NOT_ALLOWED", 
                  "Operación no permitida '" + operation + "': " + reason);
        }
    }
    
    /**
     * Error 115: Configuración bloqueada
     */
    public static class ConfigLockedException extends ConfigException {
        public ConfigLockedException(String operation) {
            super(115, "CONFIG_LOCKED", 
                  "No se puede realizar la operación '" + operation + 
                  "' porque la configuración está bloqueada");
        }
    }
}