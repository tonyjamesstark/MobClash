package com.example.mobclash.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LanguageManagerTest {

    @Mock
    private JavaPlugin plugin;

    @TempDir
    Path tempDir;

    private LanguageManager languageManager;

    @BeforeEach
    void setUp() throws IOException {
        File dataFolder = tempDir.toFile();
        File langFile = new File(dataFolder, "language.yml");

        when(plugin.getDataFolder()).thenReturn(dataFolder);
        doAnswer(invocation -> {
            String resource = invocation.getArgument(0);
            boolean replace = invocation.getArgument(1);
            if (!replace && langFile.exists()) {
                return null;
            }
            // Create a simple test language file
            try (FileWriter writer = new FileWriter(langFile)) {
                writer.write("test-message: \"&aTest message: {0}\"\n");
                writer.write("no-permission: \"&cNo permission!\"\n");
                writer.write("multiple-placeholders: \"&ePlayer {0} at {1}, {2}, {3}\"\n");
            }
            return null;
        }).when(plugin).saveResource(anyString(), anyBoolean());

        languageManager = new LanguageManager(plugin);
    }

    @Test
    void testGetMessageSimple() {
        String message = languageManager.getMessage("no-permission");
        
        assertEquals("§cNo permission!", message);
    }

    @Test
    void testGetMessageWithSingleReplacement() {
        String message = languageManager.getMessage("test-message", "World");
        
        assertEquals("§aTest message: World", message);
    }

    @Test
    void testGetMessageWithMultipleReplacements() {
        String message = languageManager.getMessage("multiple-placeholders", "Steve", 100, 64, 200);
        
        assertEquals("§ePlayer Steve at 100, 64, 200", message);
    }

    @Test
    void testGetMessageMissingKey() {
        String message = languageManager.getMessage("nonexistent-key");
        
        assertTrue(message.contains("Missing translation"));
        assertTrue(message.contains("nonexistent-key"));
    }

    @Test
    void testColorCodeReplacement() {
        String message = languageManager.getMessage("test-message", "test");
        
        assertTrue(message.startsWith("§a"));
        assertFalse(message.contains("&a"));
    }
}