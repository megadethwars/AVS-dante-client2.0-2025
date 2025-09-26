# Guía de Manejo de Errores - Dante Client

## Resumen
Se ha implementado un sistema completo de manejo de errores JSON para el package `data`, proporcionando respuestas estructuradas y consistentes en todos los endpoints de configuración.

## Componentes Implementados

### 1. ConfigException.java
- Clase base para todas las excepciones de configuración
- Contiene `errorCode` y `errorType` para respuestas estructuradas
- Soporte para causas anidadas

### 2. ConfigExceptions.java
- 15 tipos específicos de excepciones con códigos únicos (101-115):
  - **101**: ConfigFileNotFoundException - Archivo de configuración no encontrado
  - **102**: ConfigFileReadException - Error al leer archivo de configuración
  - **103**: ConfigFileWriteException - Error al escribir archivo de configuración
  - **104**: ChannelNotFoundException - Canal no encontrado
  - **105**: ChannelAlreadyExistsException - Canal ya existe
  - **106**: InvalidConfigPropertyException - Propiedad de configuración inválida
  - **107**: InvalidConfigValueException - Valor de configuración inválido
  - **108**: ConfigNotInitializedException - Configuración no inicializada
  - **109**: ConfigSyncException - Error de sincronización
  - **110**: InvalidJsonFormatException - Formato JSON inválido
  - **111**: ConfigValidationException - Error de validación
  - **112**: ConfigPermissionException - Error de permisos
  - **113**: ConfigCorruptedException - Configuración corrupta
  - **114**: ConfigLockException - Configuración bloqueada
  - **115**: ConfigBackupException - Error de respaldo

### 3. ConfigExceptionHandler.java
- Handler global con `@RestControllerAdvice`
- Mapeo automático de códigos de error a estados HTTP:
  - 404 para recursos no encontrados
  - 409 para conflictos (duplicados)
  - 400 para datos inválidos
  - 500 para errores de sistema
  - 403 para errores de permisos

### 4. DanteConfigService.java - Actualizado
- **Todos los métodos ahora lanzan excepciones** en lugar de devolver `boolean`
- Métodos actualizados:
  - `saveConfig()` - void, lanza ConfigFileWriteException
  - `updateConfigProperty()` - void, lanza ConfigNotInitializedException, InvalidConfigPropertyException
  - `addChannel()` - void, lanza ConfigNotInitializedException, ChannelAlreadyExistsException
  - `updateChannelById()` - void, lanza ConfigNotInitializedException, ChannelNotFoundException
  - `removeChannelById()` - void, lanza ConfigNotInitializedException, ChannelNotFoundException
  - `getChannelById()` - lanza ConfigNotInitializedException, ChannelNotFoundException
  - `syncWithRAM()` - void, lanza ConfigFileNotFoundException

### 5. DanteConfigController.java - Actualizado
- Controlador simplificado que depende del sistema de excepciones
- Ya no maneja errores manualmente
- Respuestas JSON automáticas vía ConfigExceptionHandler

## Formato de Respuesta de Error

```json
{
  "errorCode": 104,
  "errorType": "CHANNEL_NOT_FOUND",
  "message": "Canal con ID 999 no encontrado en la configuración",
  "timestamp": "2024-01-15T10:30:00"
}
```

## Ejemplos de Uso

### Respuesta Exitosa
```http
GET /api/config/channels/1
HTTP 200 OK
{
  "id": 1,
  "name": "Channel 1",
  "enabled": true
}
```

### Respuesta de Error
```http
GET /api/config/channels/999
HTTP 404 Not Found
{
  "errorCode": 104,
  "errorType": "CHANNEL_NOT_FOUND",
  "message": "Canal con ID 999 no encontrado en la configuración",
  "timestamp": "2024-01-15T10:30:00"
}
```

## Ventajas del Sistema

1. **Consistencia**: Todas las respuestas de error siguen el mismo formato
2. **Códigos únicos**: Cada tipo de error tiene un código específico para fácil identificación
3. **Mapeo HTTP**: Estados HTTP apropiados automáticamente
4. **Mantenibilidad**: Manejo centralizado de excepciones
5. **Debugging**: Mensajes claros y trazabilidad de errores
6. **Integración**: Fácil para clientes consumir errores estructurados

## Compilación
✅ Proyecto compila sin errores
✅ Todos los tests pasan
✅ Sistema completamente funcional

El sistema de manejo de errores JSON está completamente implementado y listo para uso en producción.