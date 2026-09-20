package io.tjs.mobclash.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LanguageManagerTest {

  @Mock private JavaPlugin plugin;

  @TempDir Path tempDir;

  private LanguageManager languageManager;

  @BeforeEach
  void setUp() throws IOException {
    File dataFolder = tempDir.toFile();
    when(plugin.getDataFolder()).thenReturn(dataFolder);

    // Create a test language.yml file
    File langFile = new File(dataFolder, "language.yml");
    try (FileWriter writer = new FileWriter(langFile)) {
      writer.write("test-message: \"&aTest message\"\n");
      writer.write("test-with-placeholder: \"Hello {0}!\"\n");
      writer.write("test-multiple-placeholders: \"{0} has {1} kills\"\n");
    }

    languageManager = new LanguageManager(plugin);
  }

  @Test
  void testGetMessageSimple() {
    String message = languageManager.getMessage("test-message");
    assertEquals("§aTest message", message);
  }

  @Test
  void testGetMessageWithPlaceholder() {
    String message = languageManager.getMessage("test-with-placeholder", "World");
    assertEquals("Hello World!", message);
  }

  @Test
  void testGetMessageWithMultiplePlaceholders() {
    String message = languageManager.getMessage("test-multiple-placeholders", "Player", 42);
    assertEquals("Player has 42 kills", message);
  }

  @Test
  void testGetMessageMissingKey() {
    String message = languageManager.getMessage("non-existent-key");
    assertTrue(message.contains("Missing translation"));
    assertTrue(message.contains("non-existent-key"));
  }

  @Test
  void testColorCodeConversion() {
    String message = languageManager.getMessage("test-message");
    // & should be converted to §
    assertTrue(message.startsWith("§a"));
    assertFalse(message.contains("&a"));
  }
}
