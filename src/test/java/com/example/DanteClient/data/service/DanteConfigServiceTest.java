package com.example.DanteClient.data.service;

import com.example.DanteClient.data.model.DanteConfig;
import com.example.DanteClient.data.model.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DanteConfigServiceTest {
    
    private DanteConfigService configService;
    
    @BeforeEach
    void setUp() {
        configService = new DanteConfigService();
    }
    
    @Test
    void testCreateAndReadConfig() {
        // Crear configuración por defecto
        DanteConfig defaultConfig = configService.createDefaultConfig();
        
        assertNotNull(defaultConfig);
        assertEquals("192.168.1.100", defaultConfig.getServer());
        assertEquals("8080", defaultConfig.getPort());
        assertEquals(3, defaultConfig.getChannels().size());
        
        // Leer la configuración
        Optional<DanteConfig> readConfig = configService.readConfig();
        assertTrue(readConfig.isPresent());
        assertEquals(defaultConfig.getServer(), readConfig.get().getServer());
    }
    
    @Test
    void testUpdateProperty() {
        // Crear configuración
        configService.createDefaultConfig();
        
        // Actualizar una propiedad
        configService.updateConfigProperty("server", "192.168.1.200");
        
        // Verificar que se actualizó
        Optional<DanteConfig> config = configService.readConfig();
        assertTrue(config.isPresent());
        assertEquals("192.168.1.200", config.get().getServer());
    }
    
    @Test
    void testChannelOperations() {
        // Crear configuración
        configService.createDefaultConfig();
        
        // Añadir un canal
        configService.addChannel("Test Channel", true);
        
        // Verificar que se añadió
        Optional<DanteConfig> config = configService.readConfig();
        assertTrue(config.isPresent());
        assertEquals(4, config.get().getChannels().size());
        
        // Actualizar el canal por ID
        configService.updateChannelById(4, "Updated Channel", false);
        // No assertion on return value since the method returns void
        
        // Verificar la actualización
        Channel updatedChannel = configService.getChannelById(4);
        assertNotNull(updatedChannel);
        assertEquals("Updated Channel", updatedChannel.getName());
        assertFalse(updatedChannel.isEnabled());
        
        // Eliminar canal por ID
        configService.removeChannelById(4);
        
        // Verificar que se eliminó
        Channel deletedChannel = configService.getChannelById(4);
        assertNull(deletedChannel);
    }
}