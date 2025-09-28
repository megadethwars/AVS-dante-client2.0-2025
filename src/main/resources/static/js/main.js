// Variables para las conexiones WebSocket
let volumeSocket;
let threadSocket;

// Inicializar las conexiones WebSocket
function initWebSockets() {
    // WebSocket para control de volumen
    volumeSocket = new WebSocket('ws://localhost:8080/ws/volume');
    volumeSocket.onopen = () => updateConnectionStatus('Conectado - Control de Volumen');
    volumeSocket.onclose = () => updateConnectionStatus('Desconectado - Control de Volumen');
    volumeSocket.onerror = () => console.error('Error en la conexión de volumen');

    // WebSocket para monitoreo de hilos
    threadSocket = new WebSocket('ws://localhost:8080/ws/thread');
    threadSocket.onopen = () => updateConnectionStatus('Conectado - Monitoreo de Hilos');
    threadSocket.onclose = () => updateConnectionStatus('Desconectado - Monitoreo de Hilos');
    threadSocket.onerror = () => console.error('Error en la conexión de hilos');

    // Manejar mensajes recibidos del servidor
    threadSocket.onmessage = (event) => {
        const threadStatus = document.getElementById('threadStatus');
        const data = JSON.parse(event.data);
        // Actualizar la interfaz con la información de los hilos
        threadStatus.innerHTML = `<pre>${JSON.stringify(data, null, 2)}</pre>`;
    };
}

// Función para ajustar el volumen
function adjustVolume(action) {
    if (volumeSocket && volumeSocket.readyState === WebSocket.OPEN) {
        volumeSocket.send(JSON.stringify({
            command: action
        }));
    } else {
        console.error('La conexión WebSocket para volumen no está disponible');
    }
}

// Función para actualizar el estado de la conexión en la interfaz
function updateConnectionStatus(status) {
    document.getElementById('connectionStatus').textContent = status;
}

// Iniciar las conexiones WebSocket cuando se carga la página
window.addEventListener('load', initWebSockets);

// Reconexión automática cuando se pierde la conexión
function setupReconnection(socket, socketType) {
    const reconnectInterval = 5000; // 5 segundos
    
    socket.onclose = () => {
        updateConnectionStatus(`Desconectado - ${socketType}`);
        setTimeout(() => {
            console.log(`Intentando reconectar ${socketType}...`);
            initWebSockets();
        }, reconnectInterval);
    };
}