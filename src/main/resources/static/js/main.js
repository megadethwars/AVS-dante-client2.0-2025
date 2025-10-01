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
            updateConnectionStatus(`Desconectado - ${type}`);
        };

        if (type === 'Monitoreo de Hilos') {
            ws.onmessage = (event) => {
                const threadStatus = document.getElementById('threadStatus');
                const data = JSON.parse(event.data);
                // Actualizar la interfaz con la información de los hilos
                threadStatus.innerHTML = `<pre>${JSON.stringify(data, null, 2)}</pre>`;
            };
        } else if (type === 'Control de Volumen') {
            ws.onmessage = (event) => {
                console.log('Mensaje recibido en socket de volumen:', event.data);
                try {
                    const data = JSON.parse(event.data);
                    if (data.type === 'volume' && data.channelId) {
                        console.log(`Actualizando volumen en UI: Canal ${data.channelId}, Volumen ${data.volumeLevel}`);
                        const volumeSlider = document.querySelector(`[data-channel-id="${data.channelId}"] input[type="range"]`);
                        if (volumeSlider) {
                            volumeSlider.value = data.volumeLevel;
                        }
                    }
                } catch (error) {
                    console.error('Error al procesar mensaje de volumen:', error);
                }
            };
        }

        return ws;
    }

    // Inicializar ambos WebSockets
    volumeSocket = setupWebSocket('ws://localhost:8080/ws/volume', 'Control de Volumen');
    threadSocket = setupWebSocket('ws://localhost:8080/ws/thread', 'Monitoreo de Hilos');
}



function updateConecionIndicator(status) 
{
     
    const connectionIcon = document.querySelector('.connection-indicator img');

    if (connectionIcon) {
        if (status.includes('Conectado')) {
            connectionIcon.src = '/images/verde.png';
            connectionIcon.alt = 'Conexión establecida';
        } else {
            connectionIcon.src = '/images/amarillo.jpg';
            connectionIcon.alt = 'Conexión perdida';
        }
    }

}

// Función para actualizar el estado de la conexión en la interfaz
function updateConnectionStatus(status) {
    const statusElement = document.getElementById('connectionStatus');
    const connectionIcon = document.querySelector('.connection-indicator img');

    if (statusElement) {
        statusElement.textContent = status;
    }
    console.log('Entrando a la actualización de iconos:', status);
    if (connectionIcon) {
        if (status.includes('Conectado')) {
            connectionIcon.src = '../images/verde.png';
            connectionIcon.alt = 'Conexión establecida';
        } else {
            connectionIcon.src = '../images/alerta.png';
            connectionIcon.alt = 'Conexión perdida';
        }
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


function handleVolumeChange(channelId, value) {
    console.log('======= AJUSTE DE VOLUMEN =======');
    console.log('Canal ID:', channelId);
    console.log('Nuevo valor:', value);
    console.log('Tipo de channelId:', typeof channelId);
    console.log('Tipo de value:', typeof value);
    
    // Verificar que los valores son válidos
    if (!channelId || !value) {
        console.error('Valores inválidos:', { channelId, value });
        return;
    }

    // Verificar estado del socket
    if (!volumeSocket) {
        console.error('Socket no inicializado');
        return;
    }

    console.log('Estado del socket:', volumeSocket.readyState);
    
    if (volumeSocket.readyState !== WebSocket.OPEN) {
        console.error('Socket no está abierto');
        return;
    }

    try {
        // Construir el mensaje
        const message = {
            channelId: parseInt(channelId),
            volume: parseInt(value)
        };
        
        // Debug del mensaje
        console.log('Mensaje a enviar:', JSON.stringify(message, null, 2));
        
        // Enviar el mensaje
        volumeSocket.send(JSON.stringify(message));
        console.log('Mensaje enviado correctamente');
        
        // Actualizar UI
        const volumeSlider = document.querySelector(`[data-channel-id="${channelId}"] input[type="range"]`);
        if (volumeSlider) {
            volumeSlider.value = value;
            console.log('UI actualizada');
        } else {
            console.error('No se encontró el slider en el DOM');
        }
    } catch (error) {
        console.error('Error en adjustChannelVolume:', error);
        console.error('Stack:', error.stack);
    }
    
    console.log('======= FIN AJUSTE VOLUMEN =======');

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
                                oninput="handleVolumeChange(${channel.id}, this.value)"
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


// // Cargar los canales cuando se inicia la página
 window.addEventListener('load', () => {
     getAllChannelStatus();  // Llamamos a getAllChannels cuando la página se carga

     initWebSockets();
 });

/**
 * Borra la caché del navegador para este sitio.
 * Nota: No se puede borrar toda la caché del navegador desde JavaScript por seguridad,
 * pero se pueden limpiar los datos almacenados localmente (localStorage, sessionStorage, y caches de Service Workers).
 */
async function clearSiteCache() {
    // Limpiar localStorage
    localStorage.clear();

    // Limpiar sessionStorage
    sessionStorage.clear();

    // Limpiar caches de Service Workers (si existen)
    if ('caches' in window) {
        const cacheNames = await caches.keys();
        await Promise.all(cacheNames.map(name => caches.delete(name)));
    }

    // Opcional: recargar la página para aplicar los cambios
    location.reload();
}



// Ejemplo: agregar un botón para borrar la caché
const clearCacheBtn = document.createElement('button');
clearCacheBtn.textContent = 'Borrar Caché';
clearCacheBtn.style.position = 'fixed';
clearCacheBtn.style.bottom = '20px';
clearCacheBtn.style.right = '20px';
clearCacheBtn.style.zIndex = '9999';
clearCacheBtn.onclick = clearSiteCache;
document.body.appendChild(clearCacheBtn);