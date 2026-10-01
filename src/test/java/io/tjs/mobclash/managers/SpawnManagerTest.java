package io.tjs.mobclash.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.tjs.mobclash.DataFile;
import io.tjs.mobclash.MobClashPlugin;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
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
    lenient().when(plugin.getLogger()).thenReturn(Logger.getLogger("SpawnManagerTest"));
    spawnManager =
        new SpawnManager(plugin, new DataFile(plugin, "spawns.yml", SpawnManager.FORMAT));
  }

  /** spawns.yml as it was last written to disk. */
  private YamlConfiguration saved() {
    return YamlConfiguration.loadConfiguration(dataFolder.resolve("spawns.yml").toFile());
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

  @Test
  void removeSpawnPointRemovesThePointAtThatIndexAndSavesTheRest() {
    when(world.getName()).thenReturn("world");
    Location loc1 = new Location(world, 100, 64, 100);
    Location loc2 = new Location(world, 200, 64, 200);
    Location loc3 = new Location(world, 300, 64, 300);
    spawnManager.addSpawnPoint("arena", loc1);
    spawnManager.addSpawnPoint("arena", loc2);
    spawnManager.addSpawnPoint("arena", loc3);

    assertEquals(loc2, spawnManager.removeSpawnPoint("arena", 1));

    assertEquals(List.of(loc1, loc3), spawnManager.getSpawnPoints("arena"));
    YamlConfiguration saved = saved();
    assertEquals(100.0, saved.getDouble("spawn-groups.arena.point-0.x"));
    assertEquals(300.0, saved.getDouble("spawn-groups.arena.point-1.x"));
    assertFalse(saved.contains("spawn-groups.arena.point-2"));
  }

  @Test
  void removeSpawnPointOutOfRangeChangesNothing() {
    when(world.getName()).thenReturn("world");
    Location loc = new Location(world, 100, 64, 100);
    spawnManager.addSpawnPoint("arena", loc);

    assertNull(spawnManager.removeSpawnPoint("arena", 1));
    assertNull(spawnManager.removeSpawnPoint("arena", -1));
    assertNull(spawnManager.removeSpawnPoint("ghost", 0));

    assertEquals(List.of(loc), spawnManager.getSpawnPoints("arena"));
    assertEquals(100.0, saved().getDouble("spawn-groups.arena.point-0.x"));
  }

  @Test
  void removingTheLastPointByIndexRemovesTheGroup() {
    when(world.getName()).thenReturn("world");
    spawnManager.addSpawnPoint("arena", new Location(world, 100, 64, 100));

    assertNotNull(spawnManager.removeSpawnPoint("arena", 0));

    assertFalse(spawnManager.hasGroup("arena"));
    assertFalse(saved().contains("spawn-groups.arena"));
  }

  @Test
  void removeGroupChestForgetsOnlyThatChest() {
    when(world.getName()).thenReturn("world");
    Location wave2 = new Location(world, 60, 64, 60);
    spawnManager.setGroupChest("arena", "wave1", new Location(world, 50, 64, 50));
    spawnManager.setGroupChest("arena", "wave2", wave2);

    assertTrue(spawnManager.removeGroupChest("arena", "wave1"));

    assertEquals(Map.of("wave2", wave2), spawnManager.getGroupWaves("arena"));
    YamlConfiguration saved = saved();
    assertFalse(saved.contains("group-chests.arena.wave1"));
    assertEquals(60.0, saved.getDouble("group-chests.arena.wave2.x"));
  }

  @Test
  void removingTheLastChestDropsTheGroupFromTheChestGroups() {
    when(world.getName()).thenReturn("world");
    spawnManager.setGroupChest("arena", "wave1", new Location(world, 50, 64, 50));

    assertTrue(spawnManager.removeGroupChest("arena", "wave1"));

    assertEquals(Set.of(), spawnManager.getChestGroups());
    assertFalse(saved().contains("group-chests.arena"));
  }

  @Test
  void removeGroupChestThatIsNotSetChangesNothing() {
    when(world.getName()).thenReturn("world");
    spawnManager.setGroupChest("arena", "wave1", new Location(world, 50, 64, 50));

    assertFalse(spawnManager.removeGroupChest("arena", "wave9"));
    assertFalse(spawnManager.removeGroupChest("ghost", "wave1"));

    assertEquals(Set.of("wave1"), spawnManager.getGroupWaves("arena").keySet());
    assertTrue(saved().contains("group-chests.arena.wave1"));
  }

  @Test
  void removeGroupForgetsItsPointsAndChestsAndKeepsOtherGroups() {
    when(world.getName()).thenReturn("world");
    spawnManager.addSpawnPoint("arena", new Location(world, 100, 64, 100));
    spawnManager.setGroupChest("arena", "wave1", new Location(world, 50, 64, 50));
    spawnManager.addSpawnPoint("other", new Location(world, 200, 64, 200));
    spawnManager.setGroupChest("other", "wave1", new Location(world, 70, 64, 70));

    assertTrue(spawnManager.removeGroup("arena"));

    assertFalse(spawnManager.hasGroup("arena"));
    assertEquals(Set.of("other"), spawnManager.getChestGroups());
    YamlConfiguration saved = saved();
    assertFalse(saved.contains("spawn-groups.arena"));
    assertFalse(saved.contains("group-chests.arena"));
    assertEquals(200.0, saved.getDouble("spawn-groups.other.point-0.x"));
    assertEquals(70.0, saved.getDouble("group-chests.other.wave1.x"));
  }

  @Test
  void removeGroupRemovesAGroupThatHasOnlyChests() {
    when(world.getName()).thenReturn("world");
    spawnManager.setGroupChest("arena", "wave1", new Location(world, 50, 64, 50));

    assertTrue(spawnManager.removeGroup("arena"));

    assertEquals(Set.of(), spawnManager.getChestGroups());
    assertFalse(saved().contains("group-chests.arena"));
  }

  @Test
  void removeGroupOfAnUnknownGroupReturnsFalse() {
    assertFalse(spawnManager.removeGroup("ghost"));
  }
}
