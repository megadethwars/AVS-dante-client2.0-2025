# Control de Volumen via WebSocket - Dante Client

## Resumen
Sistema de control de volumen en tiempo real que permite ajustar el volumen de canales activos a través de WebSocket.

## Endpoint WebSocket
```
ws://localhost:8080/volume
```

## 🎯 **Cómo Funciona**

### 1. **Enviar Comando de Volumen**
```json
{
    "channelId": 1,
    "volume": 75
}
```

### 2. **Respuestas Posibles**

#### ✅ **Éxito - Canal Activo**
```json
{
    "type": "volume_success",
    "channelId": 1,
    "volume": 75,
    "status": "applied",
    "threadStatus": "RUNNING",
    "message": "Volumen aplicado correctamente al canal 1",
    "timestamp": "2024-01-15T15:30:45"
}
```

#### ❌ **Error - Canal No Activo**
```json
{
    "type": "volume_error",
    "errorCode": "CHANNEL_NOT_ACTIVE",
    "channelId": 1,
    "requestedVolume": 75,
    "message": "El canal 1 no está activo. Inicia el thread del canal primero.",
    "suggestion": "Usa POST /api/threads/channel/1 para activar el canal",
    "timestamp": "2024-01-15T15:30:45"
}
```

#### ❌ **Error - Volumen Fuera de Rango**
```json
{
    "type": "error",
    "errorCode": "INVALID_VOLUME_RANGE",
    "message": "El volumen debe estar entre 0 y 100. Recibido: 150",
    "sessionId": "abc123",
    "timestamp": "2024-01-15T15:30:45"
}
```

#### ❌ **Error - Formato JSON Inválido**
```json
{
    "type": "error",
    "errorCode": "INVALID_FORMAT",
    "message": "Formato de mensaje no válido. Se esperaba: {\"channelId\":1,\"volume\":60}",
    "sessionId": "abc123",
    "timestamp": "2024-01-15T15:30:45"
}
```

## 🔄 **Flujo de Trabajo Completo**

### Paso 1: Iniciar Thread del Canal
```bash
curl -X POST http://localhost:8080/api/threads/channel/1
```

### Paso 2: Conectar WebSocket
```javascript
const ws = new WebSocket('ws://localhost:8080/volume');

ws.onopen = function() {
    console.log('Conectado al WebSocket de volumen');
};

ws.onmessage = function(event) {
    const data = JSON.parse(event.data);
    console.log('Respuesta:', data);
};
```

### Paso 3: Enviar Comando de Volumen
```javascript
ws.send(JSON.stringify({
    "channelId": 1,
    "volume": 75
}));
```

## 📊 **Monitoreo en Tiempo Real**

El sistema también envía actualizaciones broadcast:
```json
{
    "type": "volume",
    "channelId": 1,
    "volumeLevel": 75,
    "timestamp": "2024-01-15T15:30:45"
}
```

## 🎮 **Ejemplo Cliente HTML/JavaScript**

```html
<!DOCTYPE html>
<html>
<head>
    <title>Control de Volumen Dante</title>
</head>
<body>
    <h1>Control de Volumen</h1>
    <div>
        <label>Canal ID: <input type="number" id="channelId" value="1"></label>
        <label>Volumen: <input type="range" id="volume" min="0" max="100" value="50"></label>
        <span id="volumeValue">50</span>%
        <button onclick="sendVolume()">Aplicar</button>
    </div>
    <div id="status"></div>
    
    <script>
        const ws = new WebSocket('ws://localhost:8080/volume');
        const statusDiv = document.getElementById('status');
        const volumeSlider = document.getElementById('volume');
        const volumeValue = document.getElementById('volumeValue');
        
        // Actualizar display del volumen
        volumeSlider.oninput = function() {
            volumeValue.textContent = this.value;
        };
        
        ws.onopen = function() {
            statusDiv.innerHTML = '<span style="color: green;">✅ Conectado</span>';
        };
        
        ws.onmessage = function(event) {
            const data = JSON.parse(event.data);
            console.log('Mensaje recibido:', data);
            
            if (data.type === 'volume_success') {
                statusDiv.innerHTML = `<span style="color: green;">✅ ${data.message}</span>`;
            } else if (data.type === 'volume_error') {
                statusDiv.innerHTML = `<span style="color: red;">❌ ${data.message}</span>`;
            } else if (data.type === 'error') {
                statusDiv.innerHTML = `<span style="color: red;">❌ ${data.message}</span>`;
            }
        };
        
        ws.onclose = function() {
            statusDiv.innerHTML = '<span style="color: red;">❌ Desconectado</span>';
        };
        
        function sendVolume() {
            const channelId = parseInt(document.getElementById('channelId').value);
            const volume = parseInt(document.getElementById('volume').value);
            
            const command = {
                channelId: channelId,
                volume: volume
            };
            
            ws.send(JSON.stringify(command));
            statusDiv.innerHTML = '<span style="color: blue;">📤 Enviando comando...</span>';
        }
    </script>
</body>
</html>
```

## 🔧 **Validaciones Implementadas**

1. **Rango de Volumen**: 0-100
2. **Canal Activo**: Verifica que el thread esté corriendo
3. **Formato JSON**: Validación de estructura del mensaje
4. **Thread Existence**: Verifica que el canal exista en configuración

## 📈 **Características Avanzadas**

- **Thread-Safe**: Uso de `volatile` para el volumen
- **Broadcast**: Notifica a todas las conexiones WebSocket
- **Logging Detallado**: Seguimiento completo de operaciones
- **Manejo de Errores**: Respuestas estructuradas para todos los casos
- **Tiempo Real**: Aplicación inmediata del volumen al thread activo

## 🚀 **Testing**

Para probar el sistema:

1. Inicia la aplicación
2. Crea un thread: `POST /api/threads/channel/1`
3. Conecta WebSocket a `ws://localhost:8080/volume`
4. Envía comando: `{"channelId":1,"volume":75}`
5. Observa los logs del thread mostrando el nuevo volumen

¡El sistema está listo para control de volumen en tiempo real!