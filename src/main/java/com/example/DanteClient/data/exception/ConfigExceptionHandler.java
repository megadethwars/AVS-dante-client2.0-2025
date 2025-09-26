package com.example.DanteClient.data.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de errores para configuración de Dante
 */
@RestControllerAdvice(basePackages = "com.example.DanteClient.data.controller")
public class ConfigExceptionHandler {
    
    /**
     * Maneja excepciones específicas de configuración
     */
    @ExceptionHandler(ConfigException.class)
    public ResponseEntity<Map<String, Object>> handleConfigException(ConfigException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("success", false);
        errorResponse.put("error", true);
        errorResponse.put("errorCode", ex.getErrorCode());
        errorResponse.put("errorType", ex.getErrorType());
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("module", "configuration");
        
        // Determinar status HTTP basado en el código de error
        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Maneja excepciones generales no capturadas en el módulo de configuración
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("success", false);
        errorResponse.put("error", true);
        errorResponse.put("errorCode", 199);
        errorResponse.put("errorType", "CONFIG_INTERNAL_ERROR");
        errorResponse.put("message", "Error interno en módulo de configuración: " + ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("module", "configuration");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Determina el status HTTP apropiado basado en el código de error
     */
    private HttpStatus determineHttpStatus(int errorCode) {
        return switch (errorCode) {
            case 101 -> HttpStatus.NOT_FOUND;           // Archivo no encontrado
            case 102 -> HttpStatus.INTERNAL_SERVER_ERROR; // Error de lectura
            case 103 -> HttpStatus.INTERNAL_SERVER_ERROR; // Error de escritura
            case 104 -> HttpStatus.NOT_FOUND;          // Canal no encontrado
            case 105 -> HttpStatus.CONFLICT;           // Canal ya existe
            case 106 -> HttpStatus.BAD_REQUEST;        // Propiedad inválida
            case 107 -> HttpStatus.BAD_REQUEST;        // Valor inválido
            case 108 -> HttpStatus.BAD_REQUEST;        // JSON malformado
            case 109 -> HttpStatus.BAD_REQUEST;        // Límite excedido
            case 110 -> HttpStatus.BAD_REQUEST;        // ID inválido
            case 111 -> HttpStatus.SERVICE_UNAVAILABLE; // No inicializada
            case 112 -> HttpStatus.INTERNAL_SERVER_ERROR; // Error de sync
            case 113 -> HttpStatus.BAD_REQUEST;        // Datos inválidos
            case 114 -> HttpStatus.FORBIDDEN;          // Operación no permitida
            case 115 -> HttpStatus.LOCKED;             // Configuración bloqueada
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}