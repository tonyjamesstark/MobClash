package io.tjs.mobclash.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.tjs.mobclash.MobClashPlugin;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobTrackerTest {

  @Mock(lenient = true)
  private MobClashPlugin plugin;

  @Mock(lenient = true)
  private FileConfiguration config;

  @Mock(lenient = true)
  private Player player;

  @Mock(lenient = true)
  private Zombie zombie;

  @Mock(lenient = true)
  private PersistentDataContainer pdc;

  private MobTracker mobTracker;
  private UUID playerUuid;

  @BeforeEach
  void setUp() {
    playerUuid = UUID.randomUUID();

    when(plugin.getConfig()).thenReturn(config);
    when(config.contains(anyString())).thenReturn(false);
    doNothing().when(plugin).log(any(Level.class), anyString());
    doNothing().when(plugin).saveConfig();

    when(player.getUniqueId()).thenReturn(playerUuid);
    when(player.getName()).thenReturn("TestPlayer");

    when(zombie.getPersistentDataContainer()).thenReturn(pdc);

    mobTracker = new MobTracker(plugin);
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
