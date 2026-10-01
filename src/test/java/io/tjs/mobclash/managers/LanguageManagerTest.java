package io.tjs.mobclash.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
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
    when(plugin.getLogger()).thenReturn(Logger.getLogger("LanguageManagerTest"));
    when(plugin.getResource("language.yml"))
        .thenAnswer(invocation -> getClass().getResourceAsStream("/language.yml"));

    // Create a test language.yml file
    File langFile = new File(dataFolder, "language.yml");
    try (FileWriter writer = new FileWriter(langFile)) {
      writer.write("test-message: \"&aTest message\"\n");
      writer.write("test-with-placeholder: \"Hello {0}!\"\n");
      writer.write("test-multiple-placeholders: \"{0} has {1} kills\"\n");
      writer.write("no-permission: \"Locally edited\"\n");
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
  void aKeyMissingFromAnOlderCopyFallsBackToTheBundledOne() {
    // An upgraded server keeps the language.yml its first version wrote.
    assertEquals("§6MobClash Kills", languageManager.getMessage("killboard-title"));
  }

  @Test
  void anUnversionedCopyRunsEveryStep() throws IOException {
    File langFile = new File(tempDir.toFile(), "language.yml");
    try (FileWriter writer = new FileWriter(langFile)) {
      writer.write("no-permission: \"Locally edited\"\n");
      writer.write("killboard-usage: \"old\"\n");
      writer.write("help-header: \"old\"\n");
      writer.write("help-killboard: \"old\"\n");
      writer.write("chest-missing: \"&cThe configured chest no longer exists!\"\n");
      writer.write("setchest-success: \"&aSpawn egg chest '{1}' set for group '{0}'!\"\n");
      writer.write("must-look-chest: \"&cYou must be looking at a chest!\"\n");
    }

    languageManager.reload();

    YamlConfiguration saved = YamlConfiguration.loadConfiguration(langFile);
    assertEquals(2, saved.getInt("format-version"));
    assertEquals("Locally edited", saved.getString("no-permission"));
    assertFalse(saved.contains("killboard-usage"));
    assertFalse(saved.contains("help-header"));
    assertFalse(saved.contains("help-killboard"));
    assertFalse(saved.contains("chest-missing"));
    assertFalse(saved.contains("setchest-success"));
    assertFalse(saved.contains("must-look-chest"));
    assertEquals(
        "§aChest 'gear1' set for group 'mash'!",
        languageManager.getMessage("setchest-success", "mash", "gear1"));
  }

  @Test
  void aFormat1CopyLosesOnlyTheOldMustLookChestLine() throws IOException {
    File langFile = new File(tempDir.toFile(), "language.yml");
    try (FileWriter writer = new FileWriter(langFile)) {
      writer.write("format-version: 1\n");
      writer.write("no-permission: \"Locally edited\"\n");
      writer.write("must-look-chest: \"&cYou must be looking at a chest!\"\n");
      writer.write("killboard-usage: \"kept\"\n");
    }

    languageManager.reload();

    YamlConfiguration saved = YamlConfiguration.loadConfiguration(langFile);
    assertEquals(2, saved.getInt("format-version"));
    assertFalse(saved.contains("must-look-chest"));
    assertEquals("Locally edited", saved.getString("no-permission"));
    assertEquals("kept", saved.getString("killboard-usage"));
    assertEquals(
        "§cLook at a chest, barrel or shulker box!", languageManager.getMessage("must-look-chest"));
  }

  @Test
  void theBundledCopyIsAtTheCurrentFormat() {
    // A fresh install writes the bundled copy, which must not run a step meant for an older one.
    YamlConfiguration bundled =
        YamlConfiguration.loadConfiguration(
            new InputStreamReader(
                getClass().getResourceAsStream("/language.yml"), StandardCharsets.UTF_8));
    assertEquals(LanguageManager.FORMAT.size(), bundled.getInt("format-version"));
  }

  @Test
  void anUpgradedCopyNamesTheMissingChestWithTheBundledText() throws IOException {
    try (FileWriter writer = new FileWriter(new File(tempDir.toFile(), "language.yml"))) {
      writer.write("chest-missing: \"&cThe configured chest no longer exists!\"\n");
    }

    languageManager.reload();

    assertEquals(
        "§cChest 'wave3' of group 'mash' is gone: found STONE at MonsterMash (6, -26, -7)."
            + " Look at the chest and run /setchest mash wave3, or run /removechest mash wave3.",
        languageManager.getMessage(
            "chest-missing", "mash", "wave3", "STONE", "MonsterMash", 6, -26, -7));
  }

  @Test
  void aLocallyEditedMessageWinsOverTheBundledOne() {
    assertEquals("Locally edited", languageManager.getMessage("no-permission"));
  }

  @Test
  void reloadPicksUpAnEditMadeOnDisk() throws IOException {
    try (FileWriter writer = new FileWriter(new File(tempDir.toFile(), "language.yml"))) {
      writer.write("test-message: \"Edited while running\"\n");
    }

    languageManager.reload();

    assertEquals("Edited while running", languageManager.getMessage("test-message"));
  }

  @Test
  void testColorCodeConversion() {
    String message = languageManager.getMessage("test-message");
    // & should be converted to §
    assertTrue(message.startsWith("§a"));
    assertFalse(message.contains("&a"));
  }
}
