package io.tjs.mobclash.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.tjs.mobclash.DataFile;
import io.tjs.mobclash.MobClashPlugin;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpawnManagerTest {

  @Mock private MobClashPlugin plugin;

  @Mock private World world;

  @TempDir Path dataFolder;

  private SpawnManager spawnManager;

  @BeforeEach
  void setUp() {
    when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
    spawnManager = new SpawnManager(plugin, new DataFile(plugin, "spawns.yml"));
  }

  @Test
  void testAddSpawnPoint() {
    when(world.getName()).thenReturn("world");
    Location loc = new Location(world, 100, 64, 100);

    spawnManager.addSpawnPoint("test-group", loc);

    assertTrue(spawnManager.hasGroup("test-group"));
    assertEquals(1, spawnManager.getSpawnPoints("test-group").size());
    assertEquals(loc, spawnManager.getSpawnPoints("test-group").get(0));
  }

  @Test
  void testAddMultipleSpawnPoints() {
    when(world.getName()).thenReturn("world");
    Location loc1 = new Location(world, 100, 64, 100);
    Location loc2 = new Location(world, 200, 64, 200);

    spawnManager.addSpawnPoint("test-group", loc1);
    spawnManager.addSpawnPoint("test-group", loc2);

    assertEquals(2, spawnManager.getSpawnPoints("test-group").size());
  }

  @Test
  void testRemoveNearestSpawnPoint() {
    when(world.getName()).thenReturn("world");
    Location loc1 = new Location(world, 100, 64, 100);
    Location loc2 = new Location(world, 200, 64, 200);
    Location playerLoc = new Location(world, 105, 64, 105);

    spawnManager.addSpawnPoint("test-group", loc1);
    spawnManager.addSpawnPoint("test-group", loc2);

    boolean removed = spawnManager.removeNearestSpawnPoint("test-group", playerLoc);

    assertTrue(removed);
    assertEquals(1, spawnManager.getSpawnPoints("test-group").size());
    // loc2 should remain since loc1 was closer to playerLoc
    assertEquals(loc2, spawnManager.getSpawnPoints("test-group").get(0));
  }

  @Test
  void testSetGroupChest() {
    when(world.getName()).thenReturn("world");
    Location chestLoc = new Location(world, 50, 64, 50);

    spawnManager.addSpawnPoint("test-group", new Location(world, 100, 64, 100));
    spawnManager.setGroupChest("test-group", "wave1", chestLoc);

    assertEquals(chestLoc, spawnManager.getGroupChest("test-group", "wave1"));
  }

  @Test
  void testMultipleWavesPerGroup() {
    when(world.getName()).thenReturn("world");
    Location chest1 = new Location(world, 50, 64, 50);
    Location chest2 = new Location(world, 60, 64, 60);

    spawnManager.addSpawnPoint("arena", new Location(world, 100, 64, 100));
    spawnManager.setGroupChest("arena", "wave1", chest1);
    spawnManager.setGroupChest("arena", "wave2", chest2);

    assertEquals(chest1, spawnManager.getGroupChest("arena", "wave1"));
    assertEquals(chest2, spawnManager.getGroupChest("arena", "wave2"));
    assertEquals(2, spawnManager.getGroupWaves("arena").size());
  }

  @Test
  void testGetAllGroups() {
    when(world.getName()).thenReturn("world");
    Location loc1 = new Location(world, 100, 64, 100);
    Location loc2 = new Location(world, 200, 64, 200);

    spawnManager.addSpawnPoint("group1", loc1);
    spawnManager.addSpawnPoint("group2", loc2);

    Map<String, List<Location>> groups = spawnManager.getAllGroups();

    assertEquals(2, groups.size());
    assertTrue(groups.containsKey("group1"));
    assertTrue(groups.containsKey("group2"));
  }

  @Test
  void testHasGroup() {
    when(world.getName()).thenReturn("world");
    Location loc = new Location(world, 100, 64, 100);

    spawnManager.addSpawnPoint("existing-group", loc);

    assertTrue(spawnManager.hasGroup("existing-group"));
    assertFalse(spawnManager.hasGroup("non-existing-group"));
  }

  @Test
  void testRemoveLastSpawnPointRemovesGroup() {
    when(world.getName()).thenReturn("world");
    Location loc = new Location(world, 100, 64, 100);
    Location playerLoc = new Location(world, 105, 64, 105);

    spawnManager.addSpawnPoint("test-group", loc);
    spawnManager.removeNearestSpawnPoint("test-group", playerLoc);

    assertFalse(spawnManager.hasGroup("test-group"));
  }

  @Test
  void testGetRandomSpawnPoint() {
    when(world.getName()).thenReturn("world");
    Location loc1 = new Location(world, 100, 64, 100);
    Location loc2 = new Location(world, 200, 64, 200);

    spawnManager.addSpawnPoint("test-group", loc1);
    spawnManager.addSpawnPoint("test-group", loc2);

    Location random = spawnManager.getRandomSpawnPoint("test-group");

    assertNotNull(random);
    assertTrue(random.equals(loc1) || random.equals(loc2));
  }

  @Test
  void testGetGroupWavesEmptyForNonExistentGroup() {
    Map<String, Location> waves = spawnManager.getGroupWaves("non-existent");

    assertNotNull(waves);
    assertTrue(waves.isEmpty());
  }

  @Test
  void testGetGroupChestReturnsNullForNonExistentWave() {
    when(world.getName()).thenReturn("world");
    spawnManager.addSpawnPoint("test-group", new Location(world, 100, 64, 100));

    assertNull(spawnManager.getGroupChest("test-group", "non-existent-wave"));
  }
}
