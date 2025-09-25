package com.example.DanteClient.thread.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de errores para threads de canales
 */
@RestControllerAdvice
public class ThreadExceptionHandler {
    
    /**
     * Maneja excepciones específicas de threads
     */
    @ExceptionHandler(ThreadException.class)
    public ResponseEntity<Map<String, Object>> handleThreadException(ThreadException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("success", false);
        errorResponse.put("error", true);
        errorResponse.put("errorCode", ex.getErrorCode());
        errorResponse.put("errorType", ex.getErrorType());
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        
        // Determinar status HTTP basado en el código de error
        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Maneja excepciones generales no capturadas
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("success", false);
        errorResponse.put("error", true);
        errorResponse.put("errorCode", 999);
        errorResponse.put("errorType", "INTERNAL_ERROR");
        errorResponse.put("message", "Error interno del servidor: " + ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Determina el status HTTP apropiado basado en el código de error
     */
    private HttpStatus determineHttpStatus(int errorCode) {
        return switch (errorCode) {
            case 1 -> HttpStatus.CONFLICT;           // Thread ya existe
            case 2 -> HttpStatus.INTERNAL_SERVER_ERROR; // Error de creación
            case 3 -> HttpStatus.NOT_FOUND;          // Canal no encontrado
            case 4 -> HttpStatus.BAD_REQUEST;        // Canal deshabilitado
            case 5 -> HttpStatus.NOT_FOUND;          // Thread no encontrado
            case 6 -> HttpStatus.INTERNAL_SERVER_ERROR; // Error al detener
            case 7 -> HttpStatus.SERVICE_UNAVAILABLE;   // Pool lleno
            case 8 -> HttpStatus.CONFLICT;           // Estado inválido
            case 9 -> HttpStatus.BAD_REQUEST;        // Configuración inválida
            case 10 -> HttpStatus.TOO_MANY_REQUESTS; // Límite excedido
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}