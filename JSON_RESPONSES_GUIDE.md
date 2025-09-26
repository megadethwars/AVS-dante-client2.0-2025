# Documentación de Respuestas JSON - Dante Config API

## Nuevas Respuestas Estructuradas

Todos los endpoints ahora devuelven respuestas JSON estructuradas en lugar de texto plano.

### Formato de Respuesta Exitosa

```json
{
  "success": true,
  "message": "Descripción de la operación realizada",
  "data": {
    // Información adicional específica de la operación
  },
  "timestamp": "2024-01-15 10:30:45"
}
```

## Ejemplos de Endpoints

### 1. Agregar Canal - POST /api/config/channels

**Request:**
```json
{
  "name": "Canal Audio 1",
  "enabled": true
}
```

**Response (Antes):**
```
Canal agregado exitosamente
```

**Response (Ahora):**
```json
{
  "success": true,
  "message": "Canal agregado exitosamente",
  "data": {
    "channelName": "Canal Audio 1",
    "enabled": true,
    "action": "created"
  },
  "timestamp": "2024-01-15 10:30:45"
}
```

### 2. Actualizar Canal - PUT /api/config/channels/{id}

**Request:**
```json
{
  "name": "Canal Modificado",
  "enabled": false
}
```

**Response:**
```json
{
  "success": true,
  "message": "Canal con ID 1 actualizado exitosamente",
  "data": {
    "channelId": 1,
    "updatedName": "Canal Modificado",
    "updatedEnabled": false,
    "action": "updated"
  },
  "timestamp": "2024-01-15 10:31:20"
}
```

### 3. Eliminar Canal - DELETE /api/config/channels/{id}

**Response:**
```json
{
  "success": true,
  "message": "Canal con ID 1 eliminado exitosamente",
  "data": {
    "channelId": 1,
    "action": "deleted"
  },
  "timestamp": "2024-01-15 10:32:10"
}
```

### 4. Actualizar Propiedad - PATCH /api/config/property/{propertyName}

**Request:**
```json
{
  "value": "192.168.1.200"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Propiedad 'server' actualizada exitosamente",
  "data": {
    "property": "server",
    "newValue": "192.168.1.200"
  },
  "timestamp": "2024-01-15 10:33:00"
}
```

### 5. Reiniciar Configuración - POST /api/config/reset

**Response:**
```json
{
  "success": true,
  "message": "Configuración reiniciada a valores por defecto",
  "data": {
    "server": "192.168.1.100",
    "port": "8080",
    "multicastAddress": "224.0.0.1",
    "multicastPort": "5000",
    "chunkSize": "1024",
    "timeout": "5000",
    "channelNumbers": 64,
    "frequency": 44100,
    "channels": [
      {
        "id": 1,
        "name": "Channel 1",
        "enabled": true
      },
      {
        "id": 2,
        "name": "Channel 2",
        "enabled": false
      },
      {
        "id": 3,
        "name": "Channel 3",
        "enabled": true
      }
    ]
  },
  "timestamp": "2024-01-15 10:34:15"
}
```

### 6. Estado del Sistema - GET /api/config/status

**Response:**
```json
{
  "success": true,
  "message": "Servicio de configuración Dante funcionando correctamente",
  "data": {
    "service": "DanteConfigService",
    "status": "active",
    "version": "1.0.0"
  },
  "timestamp": "2024-01-15 10:35:00"
}
```

## Respuestas de Error (sin cambios)

Las respuestas de error mantienen su formato existente:

```json
{
  "errorCode": 104,
  "errorType": "CHANNEL_NOT_FOUND",
  "message": "Canal con ID 999 no encontrado en la configuración",
  "timestamp": "2024-01-15 10:30:00"
}
```

## Ventajas de las Nuevas Respuestas

1. **Consistencia**: Formato uniforme en todas las respuestas exitosas
2. **Información Adicional**: Campo `data` con detalles específicos de cada operación
3. **Timestamp**: Marca de tiempo para auditoría y logging
4. **Fácil Parseo**: Estructura predecible para clientes API
5. **Debugging**: Información detallada sobre qué se modificó
6. **Status Claro**: Campo `success` siempre presente para verificación rápida

## Compatibilidad

- ✅ **Respuestas de Error**: Sin cambios, mantienen compatibilidad
- ⚠️ **Respuestas de Éxito**: Formato completamente nuevo (JSON vs texto plano)
- 🔄 **Migración**: Los clientes deben actualizar para parsear JSON en lugar de texto