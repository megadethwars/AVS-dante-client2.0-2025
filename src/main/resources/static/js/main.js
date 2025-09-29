// Variables para las conexiones WebSocket
let volumeSocket;
let threadSocket;

// Inicializar las conexiones WebSocket
function initWebSockets() {
    function setupWebSocket(url, type) {
        const ws = new WebSocket(url);
        
        ws.onopen = () => {
            console.log(`Conexión establecida - ${type}`);
            updateConnectionStatus(`Conectado - ${type}`);
        };

        ws.onclose = () => {
            console.log(`Conexión perdida - ${type}`);
            updateConnectionStatus(`Desconectado - ${type}`);
            // Intentar reconexión después de 2 segundos
            setTimeout(() => {
                console.log(`Intentando reconectar - ${type}...`);
                if (type === 'Control de Volumen') {
                    volumeSocket = setupWebSocket('ws://localhost:8080/ws/volume', type);
                } else {
                    threadSocket = setupWebSocket('ws://localhost:8080/ws/thread', type);
                }
            }, 2000);
        };

        ws.onerror = (error) => {
            console.error(`Error en la conexión de ${type}:`, error);
        };

        if (type === 'Monitoreo de Hilos') {
            ws.onmessage = (event) => {
                const threadStatus = document.getElementById('threadStatus');
                const data = JSON.parse(event.data);
                // Actualizar la interfaz con la información de los hilos
                threadStatus.innerHTML = `<pre>${JSON.stringify(data, null, 2)}</pre>`;
            };
        }

        return ws;
    }

    // Inicializar ambos WebSockets
    volumeSocket = setupWebSocket('ws://localhost:8080/ws/volume', 'Control de Volumen');
    threadSocket = setupWebSocket('ws://localhost:8080/ws/thread', 'Monitoreo de Hilos');
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
    const statusElement = document.getElementById('connectionStatus');
    if (statusElement) {
        statusElement.textContent = status;
    }
    // Siempre loggear el estado en la consola para debugging
    console.log('Estado de conexión:', status);
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

// Variable global para almacenar los canales
let channels = [];

// obtener los canales
async function getAllChannels() {
    try {
        const response = await fetch('/api/config');
        if (!response.ok) {
            throw new Error('Error al obtener los canales');
        }
        console.log('/api/config')
        const data = await response.json();
        if (!data.channels || !Array.isArray(data.channels)) {
            throw new Error('La respuesta no contiene un array de canales válido');
        }
        channels = data.channels;
        console.log('Canales obtenidos sin status:', channels);
        updateChannelsUI(channels);
        return channels;
    } catch (error) {
        console.error('Error en la petición de canales:', error);
        return [];
    }
}


async function getAllChannelStatus() {
    try {
        const response = await fetch('/api/config/channels/status');
        if (!response.ok) {
            throw new Error('Error al obtener los canales');
        }
        console.log('/api/config/channels/status')
        const data = await response.json();
        if (!data.channels || !Array.isArray(data.channels)) {
            throw new Error('La respuesta no contiene un array de canales válido');
        }
        channels = data.channels;
        console.log('Canales obtenidos y status:', channels);
        updateChannelsUI(channels);
        return channels;
    } catch (error) {
        console.error('Error en la petición de canales:', error);
        return [];
    }
}

// Función para actualizar la UI con los canales
function updateChannelsUI(channels) {
    const channelsList = document.getElementById('channelsList');
    if (!channelsList) return;

    // Crear grid container
    channelsList.className = 'channels-grid';
    
    // Crear 32 espacios para canales (4x8)
    const totalSlots = 32;
    let html = '';
    
    for (let i = 0; i < totalSlots; i++) {
        const channel = channels[i] || null;
        if (channel) {
            html += `
                <div class="channel-item" data-channel-id="${channel.id}">
                    <h3>${channel.name || 'Canal ' + channel.id}</h3>
                    <div class="channel-controls">
                        <button 
                            class="power-button ${channel.isRunning ? 'on' : ''}" 
                            onclick="toggleChannel(${channel.id})"
                        >
                            ${channel.isRunning ? 'ON' : 'OFF'}
                        </button>
                        <div class="volume-control">
                            <input 
                                type="range" 
                                min="0" 
                                max="100" 
                                value="${channel.volume !== undefined ? channel.volume : 0}" 
                                onchange="adjustChannelVolume(${channel.id}, this.value)"
                            >
                        </div>
                    </div>
                </div>
            `;
        } else {
            // Espacio vacío para mantener la cuadrícula
            html += '<div class="channel-item empty"></div>';
        }
    }
    
    channelsList.innerHTML = html;
}



async function SendButtonCommandActivate(channelId) {
    try {
        // Primero, hacer la llamada a la API REST
        const response = await fetch(`/api/threads/channel/${channelId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Error al crear el thread del canal');
        }

        const data = await response.json();
        
        if (data.success) {
            // Si la API responde exitosamente, actualizar la UI
            const channelElement = document.querySelector(`[data-channel-id="${channelId}"]`);
            if (channelElement) {
                const powerButton = channelElement.querySelector('.power-button');
                if (powerButton) {
                    powerButton.classList.add('on');
                    powerButton.textContent = 'ON';
                }
            }
            // Actualizar el estado
            await getAllChannelStatus();

            // Y enviar el estado por WebSocket
            // if (volumeSocket && volumeSocket.readyState === WebSocket.OPEN) {
            //     volumeSocket.send(JSON.stringify({
            //         command: 'toggle',
            //         channelId: channelId,
            //         enabled: isEnabled
            //     }));
            // }

            // Mostrar mensaje de éxito
            console.log(`Canal ${data.channelName} activado exitosamente`);
        } else {
            throw new Error(data.message || 'Error al cambiar el estado del canal');
        }
    } catch (error) {
        console.error('Error:', error);
        // Revertir el cambio visual si hubo error
        powerButton.classList.remove('on');
        powerButton.textContent = 'OFF';
        alert(`Error al cambiar el estado del canal ${channelId}: ${error.message}`);
    }
}

async function SendButtonCommandDeactivate(channelId) {
    try {
        // Primero, hacer la llamada a la API REST
        const response = await fetch(`/api/threads/channel/${channelId}`, {
            method: 'DELETE',
            headers: {
            'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Error al crear el thread del canal');
        }

        const data = await response.json();
        
        if (data.success) {
            // Si la API responde exitosamente, actualizar la UI
            const channelElement = document.querySelector(`[data-channel-id="${channelId}"]`);
            if (channelElement) {
                const powerButton = channelElement.querySelector('.power-button');
                if (powerButton) {
                    powerButton.classList.remove('on');
                    powerButton.textContent = 'OFF';
                }
            }
            // Actualizar el estado
            await getAllChannelStatus();

            // Y enviar el estado por WebSocket
            // if (volumeSocket && volumeSocket.readyState === WebSocket.OPEN) {
            //     volumeSocket.send(JSON.stringify({
            //         command: 'toggle',
            //         channelId: channelId,
            //         enabled: isEnabled
            //     }));
            // }

            // Mostrar mensaje de éxito
            console.log(`Canal ${data.channelName} desactivado exitosamente`);
        } else {
            throw new Error(data.message || 'Error al cambiar el estado del canal');
        }
    } catch (error) {
        console.error('Error:', error);
        // Revertir el cambio visual si hubo error
        powerButton.classList.remove('on');
        powerButton.textContent = 'OFF';
        alert(`Error al cambiar el estado del canal ${channelId}: ${error.message}`);
    }
}

// Función para cambiar el estado de un canal
async function toggleChannel(channelId) {
    const channelElement = document.querySelector(`[data-channel-id="${channelId}"]`);
    if (!channelElement) return;

    const powerButton = channelElement.querySelector('.power-button');
    const isEnabled = !powerButton.classList.contains('on'); // Verificar estado actual antes de cambiar

    if(isEnabled)
        SendButtonCommandActivate(channelId);
    else
        SendButtonCommandDeactivate(channelId);
}

// Función para ajustar el volumen de un canal específico
function adjustChannelVolume(channelId, value) {
    if (volumeSocket && volumeSocket.readyState === WebSocket.OPEN) {
        volumeSocket.send(JSON.stringify({
            command: 'volume',
            channelId: channelId,
            value: parseInt(value)
        }));
    }
}

// // Cargar los canales cuando se inicia la página
 window.addEventListener('load', () => {
     getAllChannelStatus();  // Llamamos a getAllChannels cuando la página se carga

     initWebSockets();
 });
