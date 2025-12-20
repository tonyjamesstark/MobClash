package io.tjs.mobclash;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Integration tests that test the full workflow of the plugin */
@ExtendWith(MockitoExtension.class)
class MobClashPluginIT {

  @Mock(lenient = true)
  private MobClashPlugin plugin;

  @Mock(lenient = true)
  private FileConfiguration config;

  @Mock(lenient = true)
  private Player player;

  @Mock(lenient = true)
  private World world;

  @Mock(lenient = true)
  private Command command;

  @Mock(lenient = true)
  private Block block;

  @Mock(lenient = true)
  private Chest chest;

  @Mock(lenient = true)
  private Inventory inventory;

  @TempDir Path tempDir;

  private SpawnManager spawnManager;
  private LanguageManager languageManager;
  private MobTracker mobTracker;

  @BeforeEach
  void setUp() throws IOException {
    // Setup plugin mock
    File dataFolder = tempDir.toFile();
    when(plugin.getDataFolder()).thenReturn(dataFolder);
    when(plugin.getConfig()).thenReturn(config);
    when(config.contains(anyString())).thenReturn(false);

    // Mock the log method to prevent NPE
    doNothing().when(plugin).log(any(Level.class), anyString());
    doNothing().when(plugin).saveConfig();

    // Create test language.yml
    File langFile = new File(dataFolder, "language.yml");
    try (FileWriter writer = new FileWriter(langFile)) {
      writer.write("test-message: \"Test\"\n");
    }

    // Create managers
    spawnManager = new SpawnManager(plugin);
    languageManager = new LanguageManager(plugin);
    mobTracker = new MobTracker(plugin);

    when(plugin.getMobTracker()).thenReturn(mobTracker);
  }

  @Test
  void testCompleteWorkflowWithWaves() {
    // Setup world and locations
    when(world.getName()).thenReturn("world");
    Location loc1 = new Location(world, 100, 64, 100);
    Location loc2 = new Location(world, 200, 64, 200);
    Location chestLoc = new Location(world, 50, 64, 50);

    // Add spawn points
    spawnManager.addSpawnPoint("arena", loc1);
    spawnManager.addSpawnPoint("arena", loc2);

    // Set chest for wave1
    spawnManager.setGroupChest("arena", "wave1", chestLoc);

    // Verify setup
    assertTrue(spawnManager.hasGroup("arena"));
    assertEquals(2, spawnManager.getSpawnPoints("arena").size());
    assertNotNull(spawnManager.getGroupChest("arena", "wave1"));

    // Verify persistence
    Map<String, List<Location>> groups = spawnManager.getAllGroups();
    assertTrue(groups.containsKey("arena"));
    assertEquals(2, groups.get("arena").size());
  }

  @Test
  void testMultipleGroupsAndWaves() {
    when(world.getName()).thenReturn("world");
    Location arenaLoc = new Location(world, 100, 64, 100);
    Location bossLoc = new Location(world, 200, 64, 200);
    Location arenaChest1 = new Location(world, 50, 64, 50);
    Location arenaChest2 = new Location(world, 60, 64, 60);
    Location bossChest = new Location(world, 70, 64, 70);

    // Setup arena with multiple waves
    spawnManager.addSpawnPoint("arena", arenaLoc);
    spawnManager.setGroupChest("arena", "wave1", arenaChest1);
    spawnManager.setGroupChest("arena", "wave2", arenaChest2);

    // Setup boss with one wave
    spawnManager.addSpawnPoint("boss", bossLoc);
    spawnManager.setGroupChest("boss", "final", bossChest);

    // Verify
    assertEquals(2, spawnManager.getAllGroups().size());
    assertEquals(2, spawnManager.getGroupWaves("arena").size());
    assertEquals(1, spawnManager.getGroupWaves("boss").size());
  }

  @Test
  void testRemoveSpawnPoint() {
    when(world.getName()).thenReturn("world");
    Location loc1 = new Location(world, 100, 64, 100);
    Location loc2 = new Location(world, 200, 64, 200);
    Location playerLoc = new Location(world, 105, 64, 105);

    spawnManager.addSpawnPoint("test-group", loc1);
    spawnManager.addSpawnPoint("test-group", loc2);

    assertEquals(2, spawnManager.getSpawnPoints("test-group").size());

    spawnManager.removeNearestSpawnPoint("test-group", playerLoc);

    assertEquals(1, spawnManager.getSpawnPoints("test-group").size());
  }

  @Test
  void testKillTracking() {
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player.getName()).thenReturn("TestPlayer");

    // Record kills
    mobTracker.recordKill(player);
    mobTracker.recordKill(player);
    mobTracker.recordKill(player);

    // Verify kills
    assertEquals(3, mobTracker.getKills(player));
  }

  @Test
  void testKillLeaderboard() {
    Player player1 = mock(Player.class);
    Player player2 = mock(Player.class);
    UUID uuid1 = UUID.randomUUID();
    UUID uuid2 = UUID.randomUUID();

    when(player1.getUniqueId()).thenReturn(uuid1);
    when(player2.getUniqueId()).thenReturn(uuid2);
    when(player1.getName()).thenReturn("Player1");
    when(player2.getName()).thenReturn("Player2");

    // Player 1 gets 5 kills, Player 2 gets 3 kills
    for (int i = 0; i < 5; i++) mobTracker.recordKill(player1);
    for (int i = 0; i < 3; i++) mobTracker.recordKill(player2);

    List<Map.Entry<UUID, Integer>> top = mobTracker.getTopKills(10);

    assertEquals(2, top.size());
    assertEquals(uuid1, top.get(0).getKey());
    assertEquals(5, top.get(0).getValue());
    assertEquals(uuid2, top.get(1).getKey());
    assertEquals(3, top.get(1).getValue());
  }

  @Test
  void testResetKills() {
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player.getName()).thenReturn("TestPlayer");

    mobTracker.recordKill(player);
    mobTracker.recordKill(player);
    assertEquals(2, mobTracker.getKills(player));

    mobTracker.resetKills(player);
    assertEquals(0, mobTracker.getKills(player));
  }

  @Test
  void testResetAllKills() {
    Player player1 = mock(Player.class);
    Player player2 = mock(Player.class);

    when(player1.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player2.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player1.getName()).thenReturn("Player1");
    when(player2.getName()).thenReturn("Player2");

    mobTracker.recordKill(player1);
    mobTracker.recordKill(player2);

    mobTracker.resetAllKills();

    assertEquals(0, mobTracker.getKills(player1));
    assertEquals(0, mobTracker.getKills(player2));
  }

  @Test
  void testMobTagging() {
    org.bukkit.entity.Zombie zombie = mock(org.bukkit.entity.Zombie.class);
    org.bukkit.persistence.PersistentDataContainer pdc =
        mock(org.bukkit.persistence.PersistentDataContainer.class);

    when(zombie.getPersistentDataContainer()).thenReturn(pdc);

    mobTracker.tagMob(zombie, "arena", "wave1");

    verify(pdc).set(any(), eq(org.bukkit.persistence.PersistentDataType.STRING), eq("arena:wave1"));
  }
}
