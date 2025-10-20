package com.example.DanteClient.thread.model;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import com.example.DanteClient.data.util.ConfigUtil;

/**
 * Modelo que representa un thread de procesamiento de canal
 */
public class ChannelThread {
    
    private final int channelId;
    private final String channelName;
    private final LocalDateTime startTime;
    private final AtomicBoolean running;
    private CompletableFuture<Void> future; // No final para poder asignar después
    private volatile String status;
    private volatile String currentTask;
    private volatile int volume; // Volumen del canal (0-100)
    private ThreadEventListener eventListener; // Listener para notificaciones


    // thread audio
    private String serverAddress;
    private String serverPort;
    private String multicastAddress;
    private String multicastPort;
    private int chunkSize;
    private int timeout;
    private int channelNumbers;
    private int frequency;
    private String Network="default";
    //audio data
    private AudioFormat format;
    private InetAddress grupo;
    private MulticastSocket socket;
    private byte[] buffer;
    private byte[] sonido;
    private DatagramSocket Sock;
    private DatagramPacket PaqueteCliente;
    private SourceDataLine sourceline;
    byte b1,b2;
    short y;
    short contador = 0,s3;
    float pot = 1;
    int canal=0;
    private int packetCount = 0; // Contador de paquetes recibidos

    public ChannelThread(int channelId, String channelName, CompletableFuture<Void> future) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.startTime = LocalDateTime.now();
        this.running = new AtomicBoolean(true);
        this.future = future;
        this.status = "RUNNING";
        this.currentTask = "Inicializando...";
        this.volume = 0; // Volumen inicial por defecto
        StartNetworkingMulticast(); // Inicializar configuración de red
        initMulticastHandShake(); // Inicializar handshake multicast
    }
    
    /**
     * Constructor alternativo sin future (se puede asignar después)
     */
    public ChannelThread(int channelId, String channelName) {
        this(channelId, channelName, null);
    }
    
    /**
     * Detiene el thread de forma segura e inmediata
     */
    public void stop() {
        running.set(false);
        status = "STOPPING";
        currentTask = "Deteniendo thread...";
        
        // Cerrar el socket inmediatamente para interrumpir socket.receive()
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.out.println("Error al cerrar socket en stop(): " + e.getMessage());
        }
        
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
        
        status = "STOPPED";
        currentTask = "Thread detenido";
    }
    
    /**
     * Verifica si el thread está ejecutándose
     */
    public boolean isRunning() {
        return running.get() && !future.isDone();
    }
    
    /**
     * Actualiza la tarea actual que está ejecutando el thread
     */
    public void updateCurrentTask(String task) {
        this.currentTask = task;
    }
    
    /**
     * Actualiza el estado del thread
     */
    public void updateStatus(String newStatus) {
        String oldStatus = this.status;
        this.status = newStatus;
        
        // Notificar cambio de estado si hay listener
        if (eventListener != null && !oldStatus.equals(newStatus)) {
            eventListener.onThreadStatusChanged(channelId, channelName, oldStatus, newStatus);
        }
    }
    
    /**
     * Asigna el CompletableFuture al thread
     */
    public void setFuture(CompletableFuture<Void> future) {
        this.future = future;
    }
    
    /**
     * Establece el volumen del canal
     */
    public void setVolume(int volume) {
        if (volume < 0 || volume > 100) {
            throw new IllegalArgumentException("El volumen debe estar entre 0 y 100");
        }
        
        int oldVolume = this.volume;
        this.volume = volume;
        
        System.out.println("🔊 Canal " + channelId + " - Volumen cambiado de " + oldVolume + " a " + volume);
        updateCurrentTask("Volumen ajustado a " + volume + "%");
    }
    
    /**
     * Obtiene el volumen actual del canal
     */
    public int getVolume() {
        return volume;
    }
    
    /**
     * Establece el listener para eventos del thread
     */
    public void setEventListener(ThreadEventListener eventListener) {
        this.eventListener = eventListener;
    }
    

    private void StartNetworkingMulticast(){
        this.serverAddress = ConfigUtil.getServer();
        this.serverPort = ConfigUtil.getPort();
        this.multicastAddress = ConfigUtil.getMulticastAddress();
        this.multicastPort = ConfigUtil.getMulticastPort();
        this.chunkSize = Integer.parseInt(ConfigUtil.getChunkSize());
        this.timeout = Integer.parseInt(ConfigUtil.getTimeout());
        this.channelNumbers = ConfigUtil.getChannelNumbers();
        this.frequency = ConfigUtil.getFrequency();

        System.out.println("serverAddress: " + serverAddress);
        System.out.println("serverPort: " + serverPort);
        System.out.println("multicastAddress: " + multicastAddress);
        System.out.println("multicastPort: " + multicastPort);
        System.out.println("chunkSize: " + chunkSize);
        System.out.println("timeout: " + timeout);
        System.out.println("channelNumbers: " + channelNumbers);
        System.out.println("frequency: " + frequency);

        // init audio
        format = new AudioFormat(frequency, 16, 2, true, true);
        sonido=new byte[chunkSize*2];
        buffer=new byte[chunkSize*chunkSize*2];
        //buffer=new byte[256];
        DataLine.Info info=new DataLine.Info(SourceDataLine.class,format);


        try {
             sourceline = (SourceDataLine)AudioSystem.getLine(info);
             sourceline.open(format);
             FloatControl control = (FloatControl)sourceline.getControl(FloatControl.Type.MASTER_GAIN);
          
             sourceline.start();
             
         } catch (LineUnavailableException ex) {
            try {
                System.out.println("Error de audio: " + ex.getMessage());
                
                //error de audio
                
                
            } catch (Exception ex1) {
                System.out.println("Error de interrupción de audio: " + ex1.getMessage());
             
            }

         }

    }

    public int selectorAudio(int canal){
        int cuenta=0;
        System.out.println("canalS"+canal);
        cuenta=canal*chunkSize*2;
        return cuenta;
    }


    private void initMulticastHandShake(){
        try{
            if(Network.equals("default")){
            socket=new MulticastSocket(Integer.parseInt(multicastPort));
            grupo=InetAddress.getByName(multicastAddress);
            
            NetworkInterface nif = NetworkInterface.getByInetAddress(InetAddress.getByName(multicastAddress));
            //socket.joinGroup(new InetSocketAddress(grupo, Integer.parseInt(multicastPort)), nif);
            socket.joinGroup(grupo);
            socket.setSoTimeout(5000);
            
            
            socket.setSoTimeout(5000);
            }
            else{
            // Intentar obtener la interfaz cableada primero (eth0), si no, buscar otra activa
            NetworkInterface nif = null;
            try {
                // En Windows, las interfaces de red no suelen llamarse "eth0"
                // Puedes intentar con "Ethernet", "Wi-Fi", o dejarlo nulo para buscar automáticamente
                // Ejemplo: nif = NetworkInterface.getByName("Ethernet");
                // Pero normalmente es mejor dejarlo nulo y que el código busque una interfaz activa
                nif = null;
                if (nif == null || !nif.isUp()) {
                    // Si eth0 no está disponible, buscar una interfaz activa (preferir wifi)
                    for (NetworkInterface ni : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                        if (ni.isUp() && !ni.isLoopback() && !ni.isVirtual()) {
                            // Preferir wifi si existe
                            if (ni.getName().startsWith("wlan") || ni.getName().startsWith("wifi")) {
                                nif = ni;
                                break;
                            }
                            // Si no hay wifi, tomar la primera activa
                            if (nif == null) {
                                nif = ni;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("No se pudo obtener la interfaz de red preferida: " + e.getMessage());
                updateStatus("ERROR");
                updateCurrentTask("Error: " + e.getMessage());
            }
            if (nif == null) {
                throw new RuntimeException("No se encontró una interfaz de red activa para multicast");
            }
            socket = new MulticastSocket(Integer.parseInt(serverPort));
            socket.setSoTimeout(5000);
            socket.joinGroup(new InetSocketAddress(multicastAddress,Integer.parseInt(serverPort)), nif);
            
            }
            }catch(Exception ex){
                System.err.println("Error de socket en canal " + channelId + ": " + ex.getMessage());
                ex.printStackTrace();
                updateStatus("ERROR");
                updateCurrentTask("Error de conexión multicast: " + ex.getMessage());
                
                // Información adicional de diagnóstico
                System.err.println("Intentando conectar a: " + multicastAddress + ":" + multicastPort);
                System.err.println("Tipo de error: " + ex.getClass().getSimpleName());
                
                // No continuar si hay error de socket
                return;
            }
            
            
       
            PaqueteCliente = new DatagramPacket(buffer,0,buffer.length,grupo,Integer.parseInt(multicastPort));
            
            canal=selectorAudio(channelId);
            sonido=new byte[chunkSize*2];
            //cliclo while de recepcion de audio
           
            System.out.println("asignado el puerto  "+Integer.parseInt(serverPort));
            System.out.println("Iniciando audio por canal  " + channelId);
          
            System.out.println(chunkSize);
            System.out.println("SECCION DE CANAL -"+canal);
            System.out.println(sonido.length);
            
          
    }


    private void ProcessAudio() throws Exception {
        try {
            socket.receive(PaqueteCliente);
            //packetCount++; // Incrementar contador de paquetes
            byte[] rawData = PaqueteCliente.getData();
            
            // Debug: mostrar datos RAW cada 100 paquetes
           // if (packetCount % 100 == 0) {
                //System.out.println("Canal " + canal + " - Paquete " + packetCount + ": " + Arrays.toString(rawData));
           // }
            
            // Procesar formato PLANAR (128 samples = 64 por canal)
            processAudioPlanar(rawData);
            
        } catch (Exception e) {
            System.err.println("Error procesando audio en canal " + canal + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Procesa audio en formato PLANAR: Canal1[64 samples] + Canal2[64 samples]
     * Cada canal se maneja como MONO independiente
     */
    private void processAudioPlanar(byte[] rawData) {
        if (rawData.length < 10) {
            System.err.println("Datos insuficientes: " + rawData.length + " bytes (esperados 256)");
            return;
        }
        
        // Convertir bytes a samples de 16 bits
        short[] allSamples = new short[chunkSize*2]; // 256 bytes / 2 = 128 samples

        for (int i = 0; i < chunkSize*2; i++) {
            // Little-endian: byte bajo + byte alto
            int byteIndex = i * 2;
            int lowByte = rawData[byteIndex+canal] & 0xFF;
            int highByte = rawData[byteIndex + 1+canal] & 0xFF;
            allSamples[i] = (short)((highByte << 8) | lowByte);
        }
        
        // Separar canales (formato PLANAR)
        short[] canal1 = new short[chunkSize]; // Samples 0-63 (Canal 1)
        short[] canal2 = new short[chunkSize]; // Samples 64-127 (Canal 2)
        
        System.arraycopy(allSamples, 0, canal1, 0, chunkSize);     // Canal 1
        System.arraycopy(allSamples, chunkSize, canal2, 0, chunkSize); // Canal 2
        
        // Debug: mostrar algunos samples procesados
        //if (packetCount % 100 == 0) {
        //    System.out.println("Canal " + canal + " - Canal1 (primeros 5): " + Arrays.toString(Arrays.copyOf(canal1, 5)));
        //    System.out.println("Canal " + canal + " - Canal2 (primeros 5): " + Arrays.toString(Arrays.copyOf(canal2, 5)));
        //}
        
        // Seleccionar el canal específico basado en el ID del canal
        //short[] canalSeleccionado = seleccionarCanal(canal1, canal2);
        
        // Convertir el canal MONO a bytes para reproducción
        byte[] audioMono = convertirMonoABytes(canal1);

        // Reproducir audio
        if (sourceline != null && sourceline.isOpen()) {
            sourceline.write(audioMono, 0, audioMono.length);
        }
    }
    
    /**
     * Selecciona qué canal usar basado en el ID del canal
     * Canal impar (1,3,5...) = Canal 1, Canal par (2,4,6...) = Canal 2
     */
    private short[] seleccionarCanal(short[] canal1, short[] canal2) {
        // Si el ID del canal es impar, usar canal1; si es par, usar canal2
        if (canal % 2 == 1) {
            System.out.println("Seleccionado Canal 1 para canal ID " + canal);
            return canal1;
        } else {
            System.out.println("Seleccionado Canal 2 para canal ID " + canal);
            return canal2;
        }
    }
    
    /**
     * Convierte samples de audio MONO a bytes para reproducción
     */
    private byte[] convertirMonoABytes(short[] samples) {
        // Para MONO: necesitamos duplicar cada sample para crear pseudo-estéreo
        // o configurar la línea de audio como MONO
        byte[] monoBuffer = new byte[samples.length * 4]; // *4 porque duplicamos para estéreo
        
        for (int i = 0; i < samples.length; i++) {
            // Aplicar volumen
            short sample = (short)(samples[i] * volume / 100.0);
            
            // Escribir sample en ambos canales (L y R iguales para compatibilidad)
            int byteIndex = i * 4;
            
            // Canal L (mismo sample)
            monoBuffer[byteIndex] = (byte)(sample & 0xFF);        // L low byte
            monoBuffer[byteIndex + 1] = (byte)((sample >> 8) & 0xFF); // L high byte
            
            // Canal R (mismo sample para compatibilidad con dispositivos estéreo)
            monoBuffer[byteIndex + 2] = (byte)(sample & 0xFF);        // R low byte  
            monoBuffer[byteIndex + 3] = (byte)((sample >> 8) & 0xFF); // R high byte
        }
        
        return monoBuffer;
    }
    
    /**
     * Método auxiliar para crear estéreo real (no usado actualmente pero mantenido)
     */
    @SuppressWarnings("unused")
    private byte[] createStereoAudio(short[] canalL, short[] canalR) {
        byte[] stereoBuffer = new byte[chunkSize * 4]; // 64 samples * 2 canales * 2 bytes = 256 bytes
        
        for (int i = 0; i < 64; i++) {
            // Sample canal L
            short sampleL = (short)(canalL[i] * volume / 100.0); // Aplicar volumen
            stereoBuffer[i * 4] = (byte)(sampleL & 0xFF);        // L low byte
            stereoBuffer[i * 4 + 1] = (byte)((sampleL >> 8) & 0xFF); // L high byte
            
            // Sample canal R  
            short sampleR = (short)(canalR[i] * volume / 100.0); // Aplicar volumen
            stereoBuffer[i * 4 + 2] = (byte)(sampleR & 0xFF);        // R low byte
            stereoBuffer[i * 4 + 3] = (byte)((sampleR >> 8) & 0xFF); // R high byte
        }
        
        return stereoBuffer;
    }

    private void FinishAudio(){
        try {
            if (socket != null && !socket.isClosed()) {
            socket.disconnect();
            socket.close();
            }
        } catch (Exception e) {
            System.out.println("Error al finalizar el socket: " + e.getMessage());
        }
    }

    private void StopAudio(){
        try {
            if (sourceline != null) {
            sourceline.flush();
            }
        } catch (Exception e) {
            System.out.println("Error al limpiar el buffer de audio: " + e.getMessage());
        }
        try {
            if (socket != null && !socket.isClosed()) {
            socket.close();
            }
        } catch (Exception e) {
            System.out.println("Error al cerrar el socket: " + e.getMessage());
        }
    }


    /**
     * Método run que ejecuta el bucle principal del thread
     */
    public void run() {
        try {
            System.out.println("🚀 Iniciando thread para canal " + channelId + " (" + channelName + ")");
            updateStatus("RUNNING");
            updateCurrentTask("Ejecutando bucle principal");
            
            while (running.get()) {
                // if (volume > 0) {
                //     updateCurrentTask("Procesando audio - Vol: " + volume + "%");
                //     //ProcessAudio();
                // } else {
                //     updateCurrentTask("Silenciado - Vol: 0%");
                //     Thread.sleep(1000); // Esperar si está silenciado
                // }



                ProcessAudio();


                 //updateCurrentTask("Procesando audio - Vol: " + volume + "%");
                 //System.out.println("Procesando audio - Vol: " + volume + "%");
                 //Thread.sleep(1000);
            }
            
            updateStatus("FINISHED");
            updateCurrentTask("Bucle terminado");
            System.out.println("✅ Thread terminado para canal " + channelId + " (" + channelName + ")");
            
            // Notificar finalización normal
            if (eventListener != null) {
                eventListener.onThreadFinished(channelId, channelName, "Normal completion");
            }

            FinishAudio();
            StopAudio();
            
        } catch (InterruptedException e) {
            updateStatus("INTERRUPTED");
            updateCurrentTask("Thread interrumpido");
            System.out.println("⚠️ Thread interrumpido para canal " + channelId + " (" + channelName + ")");
            
            // Notificar interrupción
            if (eventListener != null) {
                eventListener.onThreadFinished(channelId, channelName, "Thread interrupted");
            }
            
        } catch (Exception e) {
            updateStatus("ERROR");
            updateCurrentTask("Error: " + e.getMessage());
            System.err.println("❌ Error en thread canal " + channelId + ": " + e.getMessage());
            
            // Notificar excepción
            if (eventListener != null) {
                eventListener.onThreadException(channelId, channelName, e.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
    
    // Getters
    public int getChannelId() {
        return channelId;
    }
    
    public String getChannelName() {
        return channelName;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public AtomicBoolean getRunningFlag() {
        return running;
    }
    
    public CompletableFuture<Void> getFuture() {
        return future;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getCurrentTask() {
        return currentTask;
    }
    
    /**
     * Obtiene información resumida del thread
     */
    public String getThreadInfo() {
        return String.format(
            "Thread Canal %d (%s) - Estado: %s - Tarea: %s - Inicio: %s",
            channelId, channelName, status, currentTask, startTime
        );
    }
    
    @Override
    public String toString() {
        return "ChannelThread{" +
                "channelId=" + channelId +
                ", channelName='" + channelName + '\'' +
                ", status='" + status + '\'' +
                ", currentTask='" + currentTask + '\'' +
                ", volume=" + volume +
                ", startTime=" + startTime +
                ", running=" + running.get() +
                '}';
    }
}