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
                try {
                    const data = JSON.parse(event.data);
                    console.log('Thread WebSocket mensaje recibido:', data);

                    // Manejar diferentes tipos de eventos
                    switch (data.type) {
                        
                        case 'threadFinished':
                            console.log(`Thread finalizado - Canal ${data.channelId} (${data.channelName}) - Razón: ${data.reason}`);
                            updateChannelStatus(data.channelId, false);
                            updateChannelStatusWithState(data.channelId, 'STOPPED');
                            break;

                        case 'thread_exception':
                            console.error(`Error en thread - Canal ${data.channelId} (${data.channelName}): ${data.errorMessage}`);
                            updateChannelStatus(data.channelId, false);
                            updateChannelStatusWithState(data.channelId, 'ERROR');
                            showThreadError(data.channelId, data.errorMessage);
                            break;

                        case 'thread_status_change':
                            console.log(`Estado del thread cambiado - Canal ${data.channelId}: ${data.oldStatus} -> ${data.newStatus}`);
                            if (data.newStatus) {
                                updateChannelStatusWithState(data.channelId, data.newStatus);
                            } else {
                                console.warn('Estado nuevo no definido en thread_status_change');
                            }
                            break;

                        case 'thread_finished':
                            console.log(`Thread finalizado - Canal ${data.channelId}`);
                            updateChannelStatus(data.channelId, false);
                            updateChannelStatusWithState(data.channelId, 'STOPPED');
                            break;

                        case 'massVolumeUpdate':
                            if (data.action === 'muteAllExcept') {
                                console.log(`Silenciando todos los canales excepto ${data.exceptedChannelId}`);
                                updateChannelsAfterMute(data.exceptedChannelId);
                            } else if (data.action === 'unmuteChannels') {
                                console.log('Restaurando volúmenes de canales');
                                refreshChannelVolumes();
                            }
                            break;

                        default:
                            console.log('Mensaje de thread no manejado:', data);
                            // Mantener el comportamiento anterior para mensajes desconocidos
                            const threadStatus = document.getElementById('threadStatus');
                            if (threadStatus) {
                                threadStatus.innerHTML = `<pre>${JSON.stringify(data, null, 2)}</pre>`;
                            }
                    }
                } catch (error) {
                    console.error('Error al procesar mensaje del WebSocket de threads:', error);
                }
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
            connectionIcon.src = '/images/alerta.png';
            connectionIcon.alt = 'Conexión establecida';
        } else {
            connectionIcon.src = '/images/alerta.png';
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
                        <button 
                            class="solo-button ${channel.SoloMutedThread ? 'on' : ''}" 
                            onclick="toggleSolo(${channel.id})"
                        >
                            ${channel.SoloMutedThread ? 'ON' : 'OFF'}
                        </button>
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

       

            // Mostrar mensaje de éxito
            console.log(`Canal ${channelId} desactivado exitosamente`);
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


async function SendButtonCommandSolo(channelId) {
    try {
        // Primero, hacer la llamada a la API REST
        const response = await fetch(`/api/volume/mute-all-except/${channelId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Error al crear el thread del canal');
        }

        const data = await response.json();

        if (data.success && data.actionPerformed == true) {
            // Si la API responde exitosamente, actualizar la UI
            const channelElement = document.querySelector(`[data-channel-id="${channelId}"]`);
            if (channelElement) {
                const soloButton = channelElement.querySelector('.solo-button');
                if (soloButton) {
                    soloButton.classList.add('on');
                    soloButton.textContent = 'ON';
                }
            }
            // Actualizar el estado
            await getAllChannelStatus();

     

            // Mostrar mensaje de éxito
            console.log(`Canal ${data.channelName} activado exitosamente`);
        } else {
            throw new Error(data.message || 'Error al cambiar el estado del canal');
        }
    } catch (error) {
        console.error('Error:', error);
        // Revertir el cambio visual si hubo error
        soloButton.classList.remove('on');
        soloButton.textContent = 'OFF';
        alert(`Error al cambiar el estado del canal ${channelId}: ${error.message}`);
    }
}

async function SendButtonCommandDeactivateSolo(channelId) {
    try {
        // Primero, hacer la llamada a la API REST
        const response = await fetch(`/api/volume/unmute-channels`, {
            method: 'PUT',
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
                const soloButton = channelElement.querySelector('.solo-button');
                if (soloButton) {
                    soloButton.classList.remove('on');
                    soloButton.textContent = 'OFF';
                }
            }
            // Actualizar el estado
            await getAllChannelStatus();

       

            // Mostrar mensaje de éxito
            console.log(`Canal ${channelId} desactivado exitosamente`);
        } else {
            throw new Error(data.message || 'Error al cambiar el estado del canal');
        }
    } catch (error) {
        console.error('Error:', error);
        // Revertir el cambio visual si hubo error
        soloButton.classList.remove('on');
        soloButton.textContent = 'OFF';
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


// Función para cambiar el estado de un canal
async function toggleSolo(channelId) {
    const channelElement = document.querySelector(`[data-channel-id="${channelId}"]`);
    if (!channelElement) return;

    const powerButton = channelElement.querySelector('.solo-button');
    const isEnabled = !powerButton.classList.contains('on'); // Verificar estado actual antes de cambiar
    console.log('Estado actual del botón Solo:', isEnabled);
    if(isEnabled)
        SendButtonCommandSolo(channelId);
    else
        SendButtonCommandDeactivateSolo(channelId);
}

// Funciones auxiliares para el manejo de eventos de threads
function updateChannelStatus(channelId, isRunning) {
    const channelElement = document.querySelector(`[data-channel-id="${channelId}"]`);
    if (channelElement) {
        const powerButton = channelElement.querySelector('.power-button');
        if (powerButton) {
            if (isRunning) {
                powerButton.classList.add('on');
                powerButton.textContent = 'ON';
            } else {
                powerButton.classList.remove('on');
                powerButton.textContent = 'OFF';
            }
        }
    }
}

function updateChannelStatusWithState(channelId, status) {
    const channelElement = document.querySelector(`[data-channel-id="${channelId}"]`);
    if (!channelElement) {
        console.warn(`No se encontró elemento para el canal ${channelId}`);
        return;
    }

    // Validar que status no sea undefined o null
    if (!status) {
        console.warn(`Estado indefinido para el canal ${channelId}`);
        return;
    }

    // Convertir a minúsculas de forma segura
    const statusLower = status.toString().toLowerCase();
    
    // Actualizar clases CSS según el estado
    channelElement.classList.remove('running', 'error', 'stopped');
    channelElement.classList.add(statusLower);

    // Actualizar el botón de encendido si corresponde
    const powerButton = channelElement.querySelector('.power-button');
    if (powerButton) {
        if (status === 'RUNNING') {
            powerButton.classList.add('on');
            powerButton.textContent = 'ON';
        } else {
            console.log('Cambiando botón a OFF para estado:', status);
            powerButton.classList.remove('on');
            powerButton.textContent = 'OFF';
        }
    }

    // Log para debugging
    console.log(`Estado actualizado para canal ${channelId}: ${status}`);
}


function showThreadError(channelId, errorMessage) {
    const channelElement = document.querySelector(`[data-channel-id="${channelId}"]`);
    if (channelElement) {
        // Agregar clase de error
        channelElement.classList.add('error');
        
        // Mostrar mensaje de error
        const errorElement = channelElement.querySelector('.error-message') || 
                           document.createElement('div');
        errorElement.className = 'error-message';
        errorElement.textContent = errorMessage;
        
        if (!channelElement.querySelector('.error-message')) {
            channelElement.appendChild(errorElement);
        }

        // Remover el mensaje después de 5 segundos
        setTimeout(() => {
            errorElement.remove();
            channelElement.classList.remove('error');
        }, 5000);
    }
}

function updateChannelsAfterMute(exceptedChannelId) {
    // Actualizar UI para reflejar el estado de mute
    document.querySelectorAll('.channel-item').forEach(channelElement => {
        const channelId = parseInt(channelElement.dataset.channelId);
        const volumeSlider = channelElement.querySelector('input[type="range"]');
        
        if (volumeSlider && channelId !== exceptedChannelId) {
            volumeSlider.value = 0;
        }
    });
}

function refreshChannelVolumes() {
    // Actualizar los volúmenes de todos los canales desde el servidor
    fetch('/api/volume/channels')
        .then(response => response.json())
        .then(data => {
            if (data.volumes) {
                Object.entries(data.volumes).forEach(([channelId, volumeInfo]) => {
                    const volumeSlider = document.querySelector(
                        `[data-channel-id="${channelId}"] input[type="range"]`
                    );
                    if (volumeSlider) {
                        volumeSlider.value = volumeInfo.volumeLevel;
                    }
                });
            }
        })
        .catch(error => console.error('Error al actualizar volúmenes:', error));
}

// Función para precargar y almacenar las imágenes en caché
async function cacheStatusImages() {
    const imagesToCache = [
        '/images/verde.png',
        '/images/rojo.png',
        '/images/alerta.png'
    ];

    // Verificar si el navegador soporta Cache Storage API
    if ('caches' in window) {
        try {
            const cache = await caches.open('status-images-cache');
            
            // Almacenar cada imagen en la caché
            for (const imageUrl of imagesToCache) {
                try {
                    const response = await fetch(imageUrl);
                    if (response.ok) {
                        await cache.put(imageUrl, response.clone());
                        console.log(`Imagen ${imageUrl} almacenada en caché`);
                    }
                } catch (error) {
                    console.error(`Error al cachear ${imageUrl}:`, error);
                }
            }
        } catch (error) {
            console.error('Error al inicializar la caché:', error);
        }
    }
}

// Función modificada para obtener imágenes de la caché si no hay conexión
async function getStatusImage(imagePath) {
    if ('caches' in window) {
        try {
            const cache = await caches.open('status-images-cache');
            const cachedResponse = await cache.match(imagePath);
            if (cachedResponse) {
                return URL.createObjectURL(await cachedResponse.blob());
            }
        } catch (error) {
            console.error('Error al obtener imagen de caché:', error);
        }
    }
    return imagePath; // Retorna la ruta original si no se encuentra en caché
}

// Modificar updateConnectionStatus para usar la caché
async function updateConnectionStatus(status) {
    const statusElement = document.getElementById('connectionStatus');
    const connectionIcon = document.querySelector('.connection-indicator img');

    if (statusElement) {
        statusElement.textContent = status;
    }

    if (connectionIcon) {
        let imagePath;
        if (status.includes('Conectado')) {
            imagePath = '/images/verde.png';
        } else if (status.includes('Error')) {
            imagePath = '/images/rojo.png';
        } else {
            imagePath = '/images/alerta.png';
        }
        
        // Intentar obtener la imagen de la caché
        connectionIcon.src = await getStatusImage(imagePath);
        connectionIcon.alt = status;
    }

    console.log('Estado de conexión:', status);
}

// Cargar los canales cuando se inicia la página
window.addEventListener('load', async () => {
    await cacheStatusImages();  // Precargar imágenes en caché
    getAllChannelStatus();      // Llamar a getAllChannels
    initWebSockets();          // Inicializar WebSockets
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