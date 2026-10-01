package io.tjs.mobclash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The migration runs once, on the first start after an upgrade, which is the worst place for a bug:
 * a server that loses its spawn points there has no second chance to get them right.
 */
@ExtendWith(MockitoExtension.class)
class DataFileTest {

  @Mock private MobClashPlugin plugin;

  @TempDir Path dataFolder;

  private DataFile spawns;
  private YamlConfiguration legacyConfig;

  @BeforeEach
  void setUp() {
    when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
    lenient().when(plugin.getLogger()).thenReturn(Logger.getLogger("DataFileTest"));
    spawns = new DataFile(plugin, "spawns.yml", SpawnManager.FORMAT);
    legacyConfig = new YamlConfiguration();
  }

  private void givenLegacySpawnPoint(String group, String point, double x) {
    legacyConfig.set("spawn-groups." + group + "." + point + ".world", "world");
    legacyConfig.set("spawn-groups." + group + "." + point + ".x", x);
    legacyConfig.set("spawn-groups." + group + "." + point + ".y", 64.0);
    legacyConfig.set("spawn-groups." + group + "." + point + ".z", 0.0);
  }

  @Test
  void adoptMovesTheWholeTreeAndLeavesNothingBehind() {
    givenLegacySpawnPoint("arena", "point-0", 1.5);
    givenLegacySpawnPoint("arena", "point-1", -2.5);
    givenLegacySpawnPoint("courtyard", "point-0", 300.0);

    assertTrue(spawns.adopt(legacyConfig, "spawn-groups"));

    assertNull(legacyConfig.get("spawn-groups"), "the section must not be left in config.yml");
    assertEquals(1.5, spawns.config().getDouble("spawn-groups.arena.point-0.x"));
    assertEquals(-2.5, spawns.config().getDouble("spawn-groups.arena.point-1.x"));
    assertEquals("world", spawns.config().getString("spawn-groups.courtyard.point-0.world"));
  }

  @Test
  void adoptLeavesOperatorSettingsAlone() {
    legacyConfig.set("logging-level", "WARNING");
    legacyConfig.set("max-mobs-per-summon", 250);
    givenLegacySpawnPoint("arena", "point-0", 1.0);

    spawns.adopt(legacyConfig, "spawn-groups");

    assertEquals("WARNING", legacyConfig.getString("logging-level"));
    assertEquals(250, legacyConfig.getInt("max-mobs-per-summon"));
    assertNull(spawns.config().get("logging-level"), "settings must not follow the data across");
  }

  @Test
  void adoptReportsThatAFreshInstallHasNothingToMove() {
    assertFalse(spawns.adopt(legacyConfig, "spawn-groups"));
    assertFalse(spawns.adopt(legacyConfig, "player-kills"));
  }

  @Test
  void adoptedDataSurvivesTheWriteToDisk() {
    givenLegacySpawnPoint("arena", "point-0", 12.25);
    spawns.adopt(legacyConfig, "spawn-groups");

    spawns.save();

    DataFile reopened = new DataFile(plugin, "spawns.yml", SpawnManager.FORMAT);
    assertEquals(12.25, reopened.config().getDouble("spawn-groups.arena.point-0.x"));
    assertEquals(64.0, reopened.config().getDouble("spawn-groups.arena.point-0.y"));
  }

  @Test
  void aFileThatDoesNotExistYetOpensWithOnlyItsFormatRatherThanFailing() {
    DataFile absent = new DataFile(plugin, "never-written.yml", SpawnManager.FORMAT);
    assertEquals(Set.of(FileFormat.KEY), absent.config().getKeys(false));
    assertEquals("never-written.yml", absent.name());
  }

  @Test
  void anUnversionedFileIsStampedOnTheNextSave() throws IOException {
    File file = dataFolder.resolve("kills.yml").toFile();
    try (FileWriter writer = new FileWriter(file)) {
      writer.write("kills: {}\n");
    }

    new DataFile(plugin, "kills.yml", MobTracker.FORMAT).save();

    assertEquals(1, YamlConfiguration.loadConfiguration(file).getInt(FileFormat.KEY));
  }

  @Test
  void aNewFileStartsAtTheCurrentFormat() {
    spawns.save();

    assertEquals(
        1,
        YamlConfiguration.loadConfiguration(dataFolder.resolve("spawns.yml").toFile())
            .getInt(FileFormat.KEY));
  }
}
