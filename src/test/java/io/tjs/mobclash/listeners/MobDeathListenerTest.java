package io.tjs.mobclash.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.KillBoard;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The drops list is a real list, as on the server, so the assertions read what the world would
 * actually drop. A custom DeathLootTable lands in the same list as vanilla loot.
 */
@ExtendWith(MockitoExtension.class)
class MobDeathListenerTest {

  @Mock private MobClashPlugin plugin;
  @Mock private MobTracker mobTracker;
  @Mock private KillBoard killBoard;
  @Mock private LanguageManager langManager;
  @Mock private Server server;
  @Mock private FileConfiguration config;
  @Mock private LivingEntity mob;
  @Mock private Player killer;
  @Mock private PlayerInventory inventory;
  @Mock private EntityDeathEvent event;

  private MobDeathListener listener;
  private List<ItemStack> drops;

  @BeforeEach
  void setUp() {
    listener = new MobDeathListener(plugin, mobTracker, killBoard, langManager);
    drops = new ArrayList<>();

    lenient().when(event.getEntity()).thenReturn(mob);
    lenient().when(event.getDrops()).thenReturn(drops);
    lenient().when(event.getEntityType()).thenReturn(EntityType.ZOMBIE);
    lenient().when(mobTracker.isMobClashMob(mob)).thenReturn(true);
    lenient().when(mob.getKiller()).thenReturn(killer);
    lenient().when(killer.getName()).thenReturn("Clasher");
    lenient().when(killer.getInventory()).thenReturn(inventory);
    lenient().when(plugin.getConfig()).thenReturn(config);
  }

  private ItemStack stackOf(int amount) {
    ItemStack item = mock(ItemStack.class);
    lenient().when(item.getAmount()).thenReturn(amount);
    return item;
  }

  @Test
  void lootGoesToTheKillerAndOnlyWhatDoesNotFitStaysOnTheGround() {
    when(config.getBoolean("loot-to-inventory", false)).thenReturn(true);
    ItemStack token = stackOf(1);
    ItemStack armour = stackOf(1);
    ItemStack leftover = stackOf(1);
    drops.addAll(List.of(token, armour));
    when(inventory.addItem(token)).thenReturn(new HashMap<>());
    when(inventory.addItem(armour)).thenReturn(new HashMap<>(Map.of(0, leftover)));

    listener.onMobDeath(event);

    assertEquals(List.of(leftover), drops);
    verify(mobTracker).recordKill(killer);
  }

  @Test
  void withTheSettingOffTheDropsAreLeftAlone() {
    when(config.getBoolean("loot-to-inventory", false)).thenReturn(false);
    ItemStack token = stackOf(1);
    drops.add(token);

    listener.onMobDeath(event);

    assertEquals(List.of(token), drops);
    verify(inventory, never()).addItem(any(ItemStack[].class));
  }

  @Test
  void aMobNotSpawnedByMobClashIsIgnored() {
    when(mobTracker.isMobClashMob(mob)).thenReturn(false);

    listener.onMobDeath(event);

    verify(mobTracker, never()).recordKill(any());
    verify(plugin, never()).getConfig();
  }

  @Test
  void aDeathNotCausedByAPlayerIsNotCountedOrLooted() {
    when(mob.getKiller()).thenReturn(null);

    listener.onMobDeath(event);

    verify(mobTracker, never()).recordKill(any());
    verify(plugin, never()).log(any(), anyString());
  }

  private void announcing(String... names) {
    when(config.getStringList("announce-kills")).thenReturn(List.of(names));
  }

  @Test
  void aListedMobsDeathIsAnnouncedToTheWholeServer() {
    announcing("wither", "minecraft:WARDEN");
    lenient().when(plugin.getServer()).thenReturn(server);
    when(langManager.getMessage("kill-announcement", "Clasher", "Warden"))
        .thenReturn("§6Clasher has slain the Warden");
    when(event.getEntityType()).thenReturn(EntityType.WARDEN);
    when(mob.getName()).thenReturn("Warden");

    listener.onMobDeath(event);

    Component expected =
        LegacyComponentSerializer.legacySection().deserialize("§6Clasher has slain the Warden");
    verify(server).broadcast(expected);
  }

  @Test
  void aMobMissingFromTheListIsNotAnnounced() {
    announcing("wither", "warden");

    listener.onMobDeath(event);

    verify(mobTracker).recordKill(killer);
    verify(plugin, never()).getServer();
  }

  @Test
  void aListedMobMobClashDidNotSummonIsNotAnnounced() {
    lenient().when(event.getEntityType()).thenReturn(EntityType.WITHER);
    when(mobTracker.isMobClashMob(mob)).thenReturn(false);

    listener.onMobDeath(event);

    verify(plugin, never()).getServer();
  }

  @Test
  void theHandlerSkipsCancelledDeathsAndRunsAfterOtherPlugins() throws NoSuchMethodException {
    // Paper lets a plugin cancel a death to revive the mob, which then never drops its loot.
    // Handling it anyway counted a kill and handed the killer loot the world never lost. Bukkit's
    // dispatcher enforces these flags, so the annotation is what a unit test can pin; a plugin
    // cancelling at HIGHEST after MobClash is still not seen.
    EventHandler handler =
        MobDeathListener.class
            .getMethod("onMobDeath", EntityDeathEvent.class)
            .getAnnotation(EventHandler.class);
    assertTrue(handler.ignoreCancelled());
    assertEquals(EventPriority.HIGHEST, handler.priority());
  }
}
