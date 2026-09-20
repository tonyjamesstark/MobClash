package io.tjs.mobclash.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.tjs.mobclash.DataFile;
import io.tjs.mobclash.MobClashPlugin;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Persistence tests that write a real spawns.yml to a temporary directory and read it back, so the
 * save-then-load path is exercised through YAML and the filesystem rather than an in-memory mock
 * that would accept anything.
 */
@ExtendWith(MockitoExtension.class)
class SpawnManagerPersistenceTest {

  @Mock private MobClashPlugin plugin;

  @Mock private World world;

  @TempDir Path dataFolder;

  private DataFile storage;
  private MockedStatic<Bukkit> bukkit;

  @BeforeEach
  void setUp() {
    // Lenient: the name-validation test never reaches a file or a world.
    lenient().when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
    lenient().when(world.getName()).thenReturn("world");
    storage = new DataFile(plugin, "spawns.yml");
    bukkit = mockStatic(Bukkit.class);
    bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
    bukkit.when(() -> Bukkit.getWorld("deleted_world")).thenReturn(null);
  }

  @AfterEach
  void tearDown() {
    bukkit.close();
  }

  /** Re-read spawns.yml from disk, so a round trip is the one the server would do on restart. */
  private DataFile reloadedFromDisk() {
    return new DataFile(plugin, "spawns.yml");
  }

  @Test
  void spawnPointsSurviveASaveAndLoadRoundTrip() {
    SpawnManager manager = new SpawnManager(plugin, storage);
    manager.addSpawnPoint("arena", new Location(world, 1.5, 64.0, -2.5));
    manager.addSpawnPoint("arena", new Location(world, 10.0, 65.0, 20.0));

    SpawnManager restored = new SpawnManager(plugin, reloadedFromDisk());

    List<Location> points = restored.getSpawnPoints("arena");
    assertEquals(2, points.size());
    assertEquals(1.5, points.get(0).getX());
    assertEquals(64.0, points.get(0).getY());
    assertEquals(-2.5, points.get(0).getZ());
    assertEquals("world", points.get(0).getWorld().getName());
  }

  @Test
  void chestsSurviveASaveAndLoadRoundTrip() {
    SpawnManager manager = new SpawnManager(plugin, storage);
    manager.addSpawnPoint("arena", new Location(world, 0.0, 64.0, 0.0));
    manager.setGroupChest("arena", "wave1", new Location(world, 5.0, 70.0, 5.0));

    SpawnManager restored = new SpawnManager(plugin, reloadedFromDisk());

    Location chest = restored.getGroupChest("arena", "wave1");
    assertNotNull(chest);
    assertEquals(5.0, chest.getX());
    assertEquals(70.0, chest.getY());
    assertEquals("world", chest.getWorld().getName());
  }

  @Test
  void aPointInAnUnloadedWorldIsSkippedRatherThanLoadedWithANullWorld() {
    storage.config().set("spawn-groups.arena.point-0.world", "deleted_world");
    storage.config().set("spawn-groups.arena.point-0.x", 1.0);
    storage.config().set("spawn-groups.arena.point-0.y", 2.0);
    storage.config().set("spawn-groups.arena.point-0.z", 3.0);
    storage.config().set("spawn-groups.arena.point-1.world", "world");
    storage.config().set("spawn-groups.arena.point-1.x", 4.0);
    storage.config().set("spawn-groups.arena.point-1.y", 5.0);
    storage.config().set("spawn-groups.arena.point-1.z", 6.0);

    SpawnManager manager = new SpawnManager(plugin, storage);

    List<Location> points = manager.getSpawnPoints("arena");
    assertEquals(1, points.size(), "the unresolvable point should be dropped, not stored as null");
    assertEquals(4.0, points.get(0).getX());
    assertNotNull(points.get(0).getWorld());
  }

  /** The regression that mattered: a bad world used to erase the config before throwing. */
  @Test
  void aSaveTriggeredAfterAnUnloadedWorldStillWritesTheSurvivingPoints() {
    storage.config().set("spawn-groups.arena.point-0.world", "deleted_world");
    storage.config().set("spawn-groups.arena.point-0.x", 1.0);
    storage.config().set("spawn-groups.arena.point-0.y", 2.0);
    storage.config().set("spawn-groups.arena.point-0.z", 3.0);

    SpawnManager manager = new SpawnManager(plugin, storage);
    assertDoesNotThrow(() -> manager.addSpawnPoint("arena", new Location(world, 7.0, 8.0, 9.0)));

    SpawnManager restored = new SpawnManager(plugin, reloadedFromDisk());

    List<Location> points = restored.getSpawnPoints("arena");
    assertEquals(1, points.size());
    assertEquals(7.0, points.get(0).getX());
  }

  @Test
  void groupNamesContainingThePathSeparatorAreRejected() {
    assertFalse(SpawnManager.isValidName("wave.one"));
    assertFalse(SpawnManager.isValidName(""));
    assertFalse(SpawnManager.isValidName(null));
    assertTrue(SpawnManager.isValidName("arena"));
    assertTrue(SpawnManager.isValidName("boss_wave-2"));
  }

  /**
   * A legacy dotted name nests as wave -> one -> point-0, so the "world" key sits a level below
   * where the loader looks. That used to reach Bukkit.getWorld(null), which throws out of the
   * constructor and fails onEnable. It must now degrade to an empty group instead.
   */
  @Test
  void aGroupPersistedWithADottedNameLoadsEmptyInsteadOfFailingStartup() {
    storage.config().set("spawn-groups.wave.one.point-0.world", "world");
    storage.config().set("spawn-groups.wave.one.point-0.x", 1.0);

    SpawnManager manager = assertDoesNotThrow(() -> new SpawnManager(plugin, storage));

    assertTrue(manager.getSpawnPoints("wave").isEmpty());
  }
}
