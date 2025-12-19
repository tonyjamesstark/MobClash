package com.example.mobclash;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.mobclash.managers.LanguageManager;
import com.example.mobclash.managers.SpawnManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Integration tests that test the full workflow of the plugin */
@ExtendWith(MockitoExtension.class)
class MobClashPluginIT {

  @Mock private JavaPlugin plugin;

  @Mock private FileConfiguration config;

  @Mock private Player player;

  @Mock private World world;

  @Mock private Command command;

  @Mock private Block block;

  @Mock private Chest chest;

  @Mock private Inventory inventory;

  @TempDir Path tempDir;

  private SpawnManager spawnManager;
  private LanguageManager languageManager;

  @BeforeEach
  void setUp() throws IOException {
    // Setup plugin mock
    File dataFolder = tempDir.toFile();
    when(plugin.getDataFolder()).thenReturn(dataFolder);
    when(plugin.getConfig()).thenReturn(config);
    when(config.contains(anyString())).thenReturn(false);

    // Create language file
    File langFile = new File(dataFolder, "language.yml");
    try (FileWriter writer = new FileWriter(langFile)) {
      writer.write("no-permission: \"&cNo permission\"\n");
      writer.write("players-only: \"&cPlayers only\"\n");
      writer.write("addspawn-success: \"&aAdded spawn to {0}! Total: {1}\"\n");
      writer.write("setchest-success: \"&aChest set for {0}\"\n");
      writer.write("summonmobs-success: \"&aSpawned {0} mobs in {1}\"\n");
      writer.write("group-not-exist: \"&cGroup {0} doesn't exist\"\n");
      writer.write("chest-not-set: \"&cChest not set for {0}\"\n");
      writer.write("must-look-chest: \"&cMust look at chest\"\n");
    }

    doAnswer(
            invocation -> {
              plugin.saveConfig();
              return null;
            })
        .when(config)
        .set(anyString(), any());

    // Initialize managers
    spawnManager = new SpawnManager(plugin);
    languageManager = new LanguageManager(plugin);

    // Setup player mock
    when(player.hasPermission(anyString())).thenReturn(true);
  }

  @Test
  void testCompleteWorkflow_AddSpawnSetChestAndSummon() {
    // Step 1: Add spawn points
    Location spawnLoc1 = new Location(world, 100, 64, 100);
    Location spawnLoc2 = new Location(world, 200, 64, 200);

    spawnManager.addSpawnPoint("arena", spawnLoc1);
    spawnManager.addSpawnPoint("arena", spawnLoc2);

    assertTrue(spawnManager.hasGroup("arena"));
    assertEquals(2, spawnManager.getSpawnPoints("arena").size());

    // Step 2: Set chest
    Location chestLoc = new Location(world, 50, 64, 50);
    spawnManager.setGroupChest("arena", chestLoc);

    assertEquals(chestLoc, spawnManager.getGroupChest("arena"));

    // Step 3: Verify data persistence
    List<Location> points = spawnManager.getSpawnPoints("arena");
    assertTrue(points.contains(spawnLoc1));
    assertTrue(points.contains(spawnLoc2));
  }

  @Test
  void testMultipleGroups() {
    Location arenaLoc = new Location(world, 100, 64, 100);
    Location bossLoc = new Location(world, 500, 64, 500);
    Location ambushLoc = new Location(world, 300, 64, 300);

    spawnManager.addSpawnPoint("arena", arenaLoc);
    spawnManager.addSpawnPoint("boss", bossLoc);
    spawnManager.addSpawnPoint("ambush", ambushLoc);

    assertTrue(spawnManager.hasGroup("arena"));
    assertTrue(spawnManager.hasGroup("boss"));
    assertTrue(spawnManager.hasGroup("ambush"));

    assertEquals(3, spawnManager.getAllGroups().size());
  }

  @Test
  void testRemoveSpawnPointsUntilEmpty() {
    Location loc1 = new Location(world, 100, 64, 100);
    Location loc2 = new Location(world, 200, 64, 200);
    Location playerLoc = new Location(world, 110, 64, 110);

    spawnManager.addSpawnPoint("test", loc1);
    spawnManager.addSpawnPoint("test", loc2);

    // Remove first point (closest to player)
    assertTrue(spawnManager.removeNearestSpawnPoint("test", playerLoc));
    assertEquals(1, spawnManager.getSpawnPoints("test").size());

    // Remove second point
    assertTrue(spawnManager.removeNearestSpawnPoint("test", playerLoc));
    assertFalse(spawnManager.hasGroup("test"));
  }

  @Test
  void testLanguageManagerWithMultipleReplacements() {
    String message = languageManager.getMessage("addspawn-success", "arena", 5);

    assertTrue(message.contains("arena"));
    assertTrue(message.contains("5"));
    assertTrue(message.startsWith("§a")); // Color code replacement
  }

  @Test
  void testRandomSpawnPointSelection() {
    Location loc1 = new Location(world, 100, 64, 100);
    Location loc2 = new Location(world, 200, 64, 200);
    Location loc3 = new Location(world, 300, 64, 300);

    spawnManager.addSpawnPoint("test", loc1);
    spawnManager.addSpawnPoint("test", loc2);
    spawnManager.addSpawnPoint("test", loc3);

    // Get multiple random points and verify they're all valid
    for (int i = 0; i < 10; i++) {
      Location random = spawnManager.getRandomSpawnPoint("test");
      assertNotNull(random);
      assertTrue(
          random.equals(loc1) || random.equals(loc2) || random.equals(loc3),
          "Random point should be one of the added locations");
    }
  }

  @Test
  void testChestForMultipleGroups() {
    Location arenaChest = new Location(world, 50, 64, 50);
    Location bossChest = new Location(world, 100, 64, 100);

    spawnManager.addSpawnPoint("arena", new Location(world, 0, 64, 0));
    spawnManager.addSpawnPoint("boss", new Location(world, 500, 64, 500));

    spawnManager.setGroupChest("arena", arenaChest);
    spawnManager.setGroupChest("boss", bossChest);

    assertEquals(arenaChest, spawnManager.getGroupChest("arena"));
    assertEquals(bossChest, spawnManager.getGroupChest("boss"));
    assertNotEquals(spawnManager.getGroupChest("arena"), spawnManager.getGroupChest("boss"));
  }

  @Test
  void testGetSpawnPointsReturnsEmptyForNonExistentGroup() {
    List<Location> points = spawnManager.getSpawnPoints("nonexistent");

    assertNotNull(points);
    assertTrue(points.isEmpty());
  }
}
