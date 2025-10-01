package com.example.DanteClient.thread.model;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
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
     * Detiene el thread de forma segura
     */
    public void stop() {
        running.set(false);
        status = "STOPPING";
        currentTask = "Deteniendo thread...";
        
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
        buffer=new byte[8192];
        DataLine.Info info=new DataLine.Info(SourceDataLine.class,format);


        try {
             sourceline = (SourceDataLine)AudioSystem.getLine(info);
             sourceline.open(format);
             FloatControl control = (FloatControl)sourceline.getControl(FloatControl.Type.MASTER_GAIN);
             //control.setValue(100.0f);
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
            socket=new MulticastSocket(Integer.parseInt(serverPort));
            grupo=InetAddress.getByName(multicastAddress);
            
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
                System.out.println("error de socket");
            }
            
            
       
            PaqueteCliente = new DatagramPacket(buffer,0,buffer.length,grupo,Integer.parseInt(serverPort));
            
            int canal=selectorAudio(channelId);
            sonido=new byte[chunkSize*2];
            //cliclo while de recepcion de audio
           
            System.out.println("asignado el puerto  "+Integer.parseInt(serverPort));
            System.out.println("Iniciando audio por canal  " + channelId);
          
            System.out.println(chunkSize);
            System.out.println("canal"+canal);
            System.out.println(sonido.length);
            
          
    }


    private void ProcessAudio(){
        try {
            socket.receive(PaqueteCliente);
                //Sock.receive(PaqueteCliente);
               
                for(int x=0;x<(chunkSize*2);x+=2){
                    sonido[x+1]= (PaqueteCliente.getData()[x+channelId]);
                    sonido[x]= (PaqueteCliente.getData()[x+1+channelId]);
                    
                    b1=sonido[x];
                    b2=sonido[x+1];
              
                    
                    y=ByteBuffer.wrap(new byte[]{b1,b2}).getShort();
                    
                    
                    y/=pot;
                    
                    y*=volume/10.0;
                   
                    s3=y;
                    b2=(byte)s3;   ///lsb
                    s3>>=8;
                    s3=(short) (s3);  ////msb
                    b1=(byte)s3;
                 
                    
                    sonido[x]=b1;          /////msb
                    sonido[x+1]=b2;            
                         
                  
                }
               
                    
                sourceline.write(sonido,0,sonido.length);
                
                System.out.println("hilo finalizado ");
            
            socket.disconnect();
            socket.close();
          
            System.out.println("FINALIZADO");
        } catch (Exception e) {
            System.out.println("Error al recibir el paquete: " + e.getMessage());
            updateStatus("ERROR");
            updateCurrentTask("Error al recibir audio: " + e.getMessage());
        }
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
                System.out.println("estoy corriendo - Canal " + channelId + " (" + channelName + ") - Volumen: " + volume + "%");
                
                // Simular procesamiento de audio con el volumen actual
                if (volume > 0) {
                    updateCurrentTask("Procesando audio - Vol: " + volume + "%");
                } else {
                    updateCurrentTask("Silenciado - Vol: 0%");
                }
                
                // Esperar 1 segundo
                Thread.sleep(1000);
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