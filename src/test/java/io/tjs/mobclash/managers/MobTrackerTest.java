package io.tjs.mobclash.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.tjs.mobclash.DataFile;
import io.tjs.mobclash.MobClashPlugin;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobTrackerTest {

  @Mock private MobClashPlugin plugin;

  @Mock private Player player;

  @Mock private Zombie zombie;

  @Mock private PersistentDataContainer pdc;

  @TempDir Path dataFolder;

  private MobTracker mobTracker;
  private UUID playerUuid;

  @BeforeEach
  void setUp() {
    playerUuid = UUID.randomUUID();

    // Shared fixture, lenient because no single test uses all of it: the kill tests never touch a
    // mob's data container and the mob tests never name a player. Stubs inside a test stay strict.
    lenient().when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());

    lenient().when(player.getUniqueId()).thenReturn(playerUuid);
    lenient().when(player.getName()).thenReturn("TestPlayer");

    lenient().when(zombie.getPersistentDataContainer()).thenReturn(pdc);

    mobTracker = new MobTracker(plugin, new DataFile(plugin, "kills.yml"));
  }

  @Test
  void theTagKeyIsPinnedSoOlderMobsStayRecognised() {
    ArgumentCaptor<NamespacedKey> key = ArgumentCaptor.forClass(NamespacedKey.class);
    mobTracker.tagMob(zombie, "arena", "wave1");
    verify(pdc).set(key.capture(), eq(PersistentDataType.STRING), eq("arena:wave1"));

    // Exactly what NamespacedKey(plugin, "mobclash_spawned") produced on 1.20, where the
    // namespace came from getName(). 1.21 takes it from namespace() instead, so this is written
    // out rather than derived. Changing either half orphans every mob tagged by an older build.
    assertEquals("mobclash", key.getValue().getNamespace());
    assertEquals("mobclash_spawned", key.getValue().getKey());
  }

  @Test
  void testTagMob() {
    mobTracker.tagMob(zombie, "arena", "wave1");

    verify(pdc).set(any(NamespacedKey.class), eq(PersistentDataType.STRING), eq("arena:wave1"));
  }

  @Test
  void testIsMobClashMob() {
    when(pdc.has(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn(true);

    assertTrue(mobTracker.isMobClashMob(zombie));
  }

  @Test
  void testIsNotMobClashMob() {
    when(pdc.has(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn(false);

    assertFalse(mobTracker.isMobClashMob(zombie));
  }

  @Test
  void testGetMobSpawnInfo() {
    when(pdc.has(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenReturn(true);
    when(pdc.get(any(NamespacedKey.class), eq(PersistentDataType.STRING)))
        .thenReturn("arena:wave1");

    String info = mobTracker.getMobSpawnInfo(zombie);

    assertEquals("arena:wave1", info);
  }

  @Test
  void testRecordKill() {
    mobTracker.recordKill(player);

    assertEquals(1, mobTracker.getKills(player));
  }

  @Test
  void testRecordMultipleKills() {
    mobTracker.recordKill(player);
    mobTracker.recordKill(player);
    mobTracker.recordKill(player);

    assertEquals(3, mobTracker.getKills(player));
  }

  @Test
  void testGetKillsByUUID() {
    mobTracker.recordKill(player);

    assertEquals(1, mobTracker.getKills(playerUuid));
  }

  @Test
  void testGetTopKills() {
    Player player2 = mock(Player.class);
    UUID uuid2 = UUID.randomUUID();
    when(player2.getUniqueId()).thenReturn(uuid2);
    when(player2.getName()).thenReturn("Player2");

    mobTracker.recordKill(player);
    mobTracker.recordKill(player);
    mobTracker.recordKill(player);
    mobTracker.recordKill(player2);

    List<Map.Entry<UUID, Integer>> top = mobTracker.getTopKills(10);

    assertEquals(2, top.size());
    assertEquals(playerUuid, top.get(0).getKey());
    assertEquals(3, top.get(0).getValue());
    assertEquals(uuid2, top.get(1).getKey());
    assertEquals(1, top.get(1).getValue());
  }

  @Test
  void testResetKills() {
    mobTracker.recordKill(player);
    mobTracker.recordKill(player);

    assertEquals(2, mobTracker.getKills(player));

    mobTracker.resetKills(player);

    assertEquals(0, mobTracker.getKills(player));
  }

  @Test
  void testResetAllKills() {
    Player player2 = mock(Player.class);
    when(player2.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player2.getName()).thenReturn("Player2");

    mobTracker.recordKill(player);
    mobTracker.recordKill(player2);

    mobTracker.resetAllKills();

    assertEquals(0, mobTracker.getKills(player));
    assertEquals(0, mobTracker.getKills(player2));
  }
}
