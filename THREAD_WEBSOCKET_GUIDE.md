# WebSocket de Notificaciones de Threads - Dante Client

## Resumen
Nuevo canal WebSocket que permite recibir notificaciones en tiempo real sobre el estado de los threads (inicio, finalización, excepciones y cambios de estado).

## Endpoint WebSocket
```
ws://localhost:8080/ws/thread
```

## 🎯 **Tipos de Notificaciones**

### 1. **Conexión Establecida**
Al conectarse, recibes el estado actual:
```json
{
  "type": "connection",
  "status": "connected",
  "sessionId": "abc123",
  "totalConnections": 2,
  "activeThreadsCount": 3,
  "activeThreads": [
    {
      "channelId": 1,
      "channelName": "Channel 1",
      "status": "RUNNING",
      "volume": 75
    }
  ],
  "timestamp": "2024-01-15T15:30:45"
}
```

### 2. **Thread Iniciado**
```json
{
  "type": "thread_started",
  "channelId": 1,
  "channelName": "Channel 1",
  "message": "Thread iniciado para canal 1 (Channel 1)",
  "timestamp": "2024-01-15T15:30:45"
}
```

### 3. **Thread Finalizado**
```json
{
  "type": "thread_finished",
  "channelId": 1,
  "channelName": "Channel 1",
  "reason": "Normal completion",
  "message": "Thread finalizado para canal 1 (Channel 1) - Razón: Normal completion",
  "timestamp": "2024-01-15T15:35:20"
}
```

### 4. **Excepción en Thread**
```json
{
  "type": "thread_exception",
  "channelId": 1,
  "channelName": "Channel 1",
  "exceptionType": "RuntimeException",
  "errorMessage": "Error procesando audio",
  "message": "Excepción en thread del canal 1 (Channel 1): Error procesando audio",
  "timestamp": "2024-01-15T15:32:10"
}
```

### 5. **Cambio de Estado**
```json
{
  "type": "thread_status_change",
  "channelId": 1,
  "channelName": "Channel 1",
  "oldStatus": "RUNNING",
  "newStatus": "STOPPING",
  "message": "Estado del thread 1 cambió de RUNNING a STOPPING",
  "timestamp": "2024-01-15T15:33:15"
}
```

## 🎮 **Comandos del Cliente**

Puedes enviar comandos al WebSocket:

### 1. **Solicitar Estado**
```json
{
  "command": "status"
}
```

**Respuesta:**
```json
{
  "type": "thread_status",
  "activeThreadsCount": 3,
  "totalConnections": 2,
  "threads": [
    {
      "channelId": 1,
      "channelName": "Channel 1",
      "status": "RUNNING",
      "currentTask": "Procesando audio - Vol: 75%",
      "volume": 75,
      "startTime": "2024-01-15T15:30:00",
      "isRunning": true
    }
  ],
  "timestamp": "2024-01-15T15:35:00"
}
```

### 2. **Listar Threads Activos**
```json
{
  "command": "list"
}
```

**Respuesta:**
```json
{
  "type": "thread_list",
  "count": 3,
  "threads": [
    {
      "channelId": 1,
      "channelName": "Channel 1",
      "status": "RUNNING",
      "volume": 75
    }
  ],
  "timestamp": "2024-01-15T15:35:00"
}
```

## 🔄 **Flujo de Eventos Automáticos**

1. **Al iniciar thread** → `thread_started`
2. **Durante ejecución** → `thread_status_change` (si cambia estado)
3. **Si hay error** → `thread_exception`
4. **Al finalizar** → `thread_finished`

## 💡 **Ejemplo Cliente JavaScript**

```html
<!DOCTYPE html>
<html>
<head>
    <title>Monitoreo de Threads Dante</title>
    <style>
        .notification { margin: 10px; padding: 10px; border-radius: 5px; }
        .started { background-color: #d4edda; color: #155724; }
        .finished { background-color: #f8d7da; color: #721c24; }
        .exception { background-color: #f8d7da; color: #721c24; }
        .status-change { background-color: #d1ecf1; color: #0c5460; }
    </style>
</head>
<body>
    <h1>Monitor de Threads Dante</h1>
    
    <div>
        <button onclick="sendCommand('status')">Estado General</button>
        <button onclick="sendCommand('list')">Listar Threads</button>
        <div id="connectionStatus">Desconectado</div>
    </div>
    
    <div id="notifications"></div>
    
    <script>
        const ws = new WebSocket('ws://localhost:8080/ws/thread');
        const notificationsDiv = document.getElementById('notifications');
        const statusDiv = document.getElementById('connectionStatus');
        
        ws.onopen = function() {
            statusDiv.innerHTML = '<span style="color: green;">✅ Conectado al monitor de threads</span>';
        };
        
        ws.onmessage = function(event) {
            const data = JSON.parse(event.data);
            console.log('Notificación recibida:', data);
            
            addNotification(data);
        };
        
        ws.onclose = function() {
            statusDiv.innerHTML = '<span style="color: red;">❌ Desconectado</span>';
        };
        
        function addNotification(data) {
            const div = document.createElement('div');
            div.className = 'notification';
            
            let cssClass = '';
            let icon = '';
            
            switch(data.type) {
                case 'connection':
                    cssClass = 'status-change';
                    icon = '🔗';
                    div.innerHTML = `${icon} Conectado - ${data.activeThreadsCount} threads activos`;
                    break;
                case 'thread_started':
                    cssClass = 'started';
                    icon = '🚀';
                    div.innerHTML = `${icon} ${data.message}`;
                    break;
                case 'thread_finished':
                    cssClass = 'finished';
                    icon = '🏁';
                    div.innerHTML = `${icon} ${data.message}`;
                    break;
                case 'thread_exception':
                    cssClass = 'exception';
                    icon = '⚠️';
                    div.innerHTML = `${icon} ${data.message}`;
                    break;
                case 'thread_status_change':
                    cssClass = 'status-change';
                    icon = '🔄';
                    div.innerHTML = `${icon} ${data.message}`;
                    break;
                case 'thread_status':
                    cssClass = 'status-change';
                    icon = '📊';
                    div.innerHTML = `${icon} Estado: ${data.activeThreadsCount} threads activos`;
                    break;
                case 'thread_list':
                    cssClass = 'status-change';
                    icon = '📋';
                    div.innerHTML = `${icon} Lista: ${data.count} threads activos`;
                    break;
            }
            
            div.className += ' ' + cssClass;
            div.innerHTML += ` <small>(${data.timestamp})</small>`;
            
            // Insertar al principio para ver las más recientes arriba
            notificationsDiv.insertBefore(div, notificationsDiv.firstChild);
            
            // Limitar a 20 notificaciones
            while (notificationsDiv.children.length > 20) {
                notificationsDiv.removeChild(notificationsDiv.lastChild);
            }
        }
        
        function sendCommand(command) {
            if (ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({command: command}));
            } else {
                alert('WebSocket no está conectado');
            }
        }
    </script>
</body>
</html>
```

## 🛠️ **Testing del Sistema**

### Caso 1: Thread Normal
1. Conectar: `ws://localhost:8080/ws/thread`
2. Crear thread: `POST /api/threads/channel/1`
3. **Esperar notificación:** `thread_started`
4. Detener thread: `DELETE /api/threads/channel/1`
5. **Esperar notificación:** `thread_finished`

### Caso 2: Thread con Excepción
1. Conectar al WebSocket
2. Crear thread en canal inexistente
3. **Esperar notificación:** `thread_exception`

### Caso 3: Monitoreo en Tiempo Real
1. Abrir múltiples pestañas con el cliente HTML
2. Crear/detener threads desde Postman
3. **Observar:** Todas las pestañas reciben notificaciones simultáneamente

## 🔧 **Características Técnicas**

- **Lazy Loading**: Resuelve dependencias circulares con `@Lazy`
- **Thread-Safe**: Uso de `ConcurrentHashMap` y `CopyOnWriteArraySet`
- **Event-Driven**: Sistema de callbacks para notificaciones automáticas
- **Broadcast**: Todas las conexiones reciben las mismas notificaciones
- **Error Handling**: Manejo robusto de desconexiones y errores JSON

## 📈 **Beneficios**

1. **Monitoreo en Tiempo Real**: Ve cuando los threads inician/terminan
2. **Debugging**: Notificaciones de excepciones inmediatas
3. **Múltiples Clientes**: Varios usuarios pueden monitorear simultáneamente
4. **Historial**: Sistema de comandos para consultar estado actual
5. **Integración**: Se conecta automáticamente con el sistema de threads existente

¡El sistema de notificaciones de threads está listo para monitoreo en tiempo real! 🎵⚡