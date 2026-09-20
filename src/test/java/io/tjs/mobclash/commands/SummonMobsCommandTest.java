package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Every guard clause in this command ends the same way -- a message and {@code return true} -- so
 * the tests assert on the message key rather than on {@code anyString()}. getMessage is stubbed to
 * echo its key back, which makes a transposed pair of guards fail instead of pass.
 *
 * <p>Only the framing stubs below are lenient. Everything a test actually exercises is strict, so
 * an unused stub reports itself instead of quietly marking a path as covered.
 */
@ExtendWith(MockitoExtension.class)
class SummonMobsCommandTest {

  @Mock private MobClashPlugin plugin;
  @Mock private SpawnManager spawnManager;
  @Mock private LanguageManager langManager;
  @Mock private MobTracker mobTracker;
  @Mock private FileConfiguration config;
  @Mock private Player player;
  @Mock private BlockCommandSender commandBlock;
  @Mock private Command command;
  @Mock private World world;
  @Mock private Block block;
  @Mock private Chest chest;
  @Mock private Inventory inventory;

  private SummonMobsCommand summonMobsCommand;
  private Location spawnLoc;
  private Location chestLoc;

  @BeforeEach
  void setUp() {
    summonMobsCommand = new SummonMobsCommand(plugin, spawnManager, langManager);
    spawnLoc = new Location(world, 100, 64, 100);
    chestLoc = new Location(world, 50, 64, 50);

    // BaseCommand logs the command name and sender on every path before anything else runs.
    lenient().when(command.getName()).thenReturn("summonmobs");
    lenient().when(player.getName()).thenReturn("TestPlayer");
    lenient().when(commandBlock.getName()).thenReturn("CommandBlock");
    lenient().when(player.hasPermission("mobclash.summon")).thenReturn(true);
    lenient()
        .when(langManager.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private boolean run(Object sender, String... args) {
    return summonMobsCommand.onCommand(
        (org.bukkit.command.CommandSender) sender, command, "summonmobs", args);
  }

  private void givenGroup(String group, String wave, List<Location> points) {
    when(spawnManager.hasGroup(group)).thenReturn(true);
    when(spawnManager.getGroupChest(group, wave)).thenReturn(chestLoc);
    when(spawnManager.getSpawnPoints(group)).thenReturn(points);
  }

  private void givenCapOf(int max) {
    when(plugin.getConfig()).thenReturn(config);
    when(config.getInt(eq("max-mobs-per-summon"), anyInt())).thenReturn(max);
  }

  /** The chest block at chestLoc. Location.getBlock() resolves through the world, so stub there. */
  private void givenChestHolding(ItemStack... contents) {
    when(world.getBlockAt(chestLoc)).thenReturn(block);
    when(block.getType()).thenReturn(Material.CHEST);
    when(block.getState()).thenReturn(chest);
    when(chest.getInventory()).thenReturn(inventory);
    when(inventory.getContents()).thenReturn(contents);
  }

  /** Make the world hand back a mob of the given type, accepted or refused by the server. */
  private LivingEntity givenSpawnOf(EntityType type, boolean accepted) {
    LivingEntity mob = mock(LivingEntity.class);
    when(mob.isValid()).thenReturn(accepted);
    when(world.spawnEntity(any(Location.class), eq(type))).thenReturn(mob);
    when(spawnManager.getRandom()).thenReturn(new Random(42));
    when(plugin.getMobTracker()).thenReturn(mobTracker);
    return mob;
  }

  @Test
  void randomModeSpawnsTheRequestedCountAtOnePoint() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    givenChestHolding(new ItemStack(Material.ZOMBIE_SPAWN_EGG, 5));
    givenSpawnOf(EntityType.ZOMBIE, true);

    assertTrue(run(player, "test-group", "wave1", "random", "3"));

    verify(world, times(3)).spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
    verify(mobTracker, times(3)).tagMob(any(), eq("test-group"), eq("wave1"));
    verify(player).sendMessage("summonmobs-success");
  }

  @Test
  void allModeSpawnsTheRequestedCountAtEveryPoint() {
    Location spawnLoc2 = new Location(world, 200, 64, 200);
    givenGroup("test-group", "wave1", List.of(spawnLoc, spawnLoc2));
    givenCapOf(500);
    givenChestHolding(new ItemStack(Material.ZOMBIE_SPAWN_EGG, 5));
    givenSpawnOf(EntityType.ZOMBIE, true);

    assertTrue(run(player, "test-group", "wave1", "all", "2"));

    verify(world, times(4)).spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
    verify(mobTracker, times(4)).tagMob(any(), eq("test-group"), eq("wave1"));
  }

  @Test
  void aCommandBlockMaySummonWithoutPermission() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    givenChestHolding(new ItemStack(Material.ZOMBIE_SPAWN_EGG, 1));
    givenSpawnOf(EntityType.ZOMBIE, true);

    assertTrue(run(commandBlock, "test-group", "wave1", "random", "1"));

    verify(world).spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
    verify(commandBlock, never()).sendMessage("no-permission");
  }

  @Test
  void anEggWhoseEnumNameDiffersFromItsEntityStillMaps() {
    // MOOSHROOM_SPAWN_EGG's entity constant is MUSHROOM_COW. Matching enum names dropped this egg
    // silently; the key-based map does not.
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    givenChestHolding(new ItemStack(Material.MOOSHROOM_SPAWN_EGG, 1));
    givenSpawnOf(EntityType.MUSHROOM_COW, true);

    assertTrue(run(player, "test-group", "wave1", "random", "1"));

    verify(world).spawnEntity(any(Location.class), eq(EntityType.MUSHROOM_COW));
  }

  @Test
  void nonEggContentsAndEmptySlotsAreIgnored() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    givenChestHolding(
        null, new ItemStack(Material.DIAMOND, 64), new ItemStack(Material.ZOMBIE_SPAWN_EGG, 1));
    givenSpawnOf(EntityType.ZOMBIE, true);

    assertTrue(run(player, "test-group", "wave1", "random", "1"));

    verify(world).spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
  }

  @Test
  void aRefusedSpawnIsNotCountedOrTagged() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    givenChestHolding(new ItemStack(Material.ZOMBIE_SPAWN_EGG, 5));
    // A protection plugin cancelled CreatureSpawnEvent: Bukkit still returns the entity object.
    givenSpawnOf(EntityType.ZOMBIE, false);

    run(player, "test-group", "wave1", "random", "3");

    verify(mobTracker, never()).tagMob(any(), anyString(), anyString());
  }

  @Test
  void tooFewArgumentsPrintsTheUsage() {
    assertTrue(run(player, "test-group"));
    verify(player).sendMessage("summonmobs-usage");
  }

  @Test
  void anUnknownModeIsRejectedBeforeAnythingIsLookedUp() {
    assertTrue(run(player, "test-group", "wave1", "sideways"));
    verify(player).sendMessage("invalid-mode");
    verify(spawnManager, never()).hasGroup(anyString());
  }

  @Test
  void aMissingGroupIsReported() {
    when(spawnManager.hasGroup("nonexistent")).thenReturn(false);
    assertTrue(run(player, "nonexistent", "wave1", "random"));
    verify(player).sendMessage("group-not-exist");
  }

  @Test
  void aWaveWithNoChestIsReported() {
    when(spawnManager.hasGroup("test-group")).thenReturn(true);
    when(spawnManager.getGroupChest("test-group", "wave1")).thenReturn(null);
    assertTrue(run(player, "test-group", "wave1", "random"));
    verify(player).sendMessage("wave-not-set");
  }

  @Test
  void aGroupWithNoSpawnPointsIsReported() {
    givenGroup("test-group", "wave1", List.of());
    assertTrue(run(player, "test-group", "wave1", "random"));
    verify(player).sendMessage("group-no-points");
  }

  @Test
  void anAmountAboveTheParseBoundIsRejected() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    assertTrue(run(player, "test-group", "wave1", "random", "101"));
    verify(player).sendMessage("invalid-amount");
    verify(world, never()).spawnEntity(any(Location.class), any(EntityType.class));
  }

  @Test
  void anAmountBelowOneIsRejected() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    assertTrue(run(player, "test-group", "wave1", "random", "0"));
    verify(player).sendMessage("invalid-amount");
  }

  @Test
  void aNonNumericAmountIsRejected() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    assertTrue(run(player, "test-group", "wave1", "random", "lots"));
    verify(player).sendMessage("invalid-number");
  }

  @Test
  void aSummonOverTheTotalCapIsRefused() {
    // 60 spawn points x 100 each = 6000, over the configured 500.
    givenGroup("test-group", "wave1", Collections.nCopies(60, spawnLoc));
    givenCapOf(500);

    run(player, "test-group", "wave1", "all", "100");

    verify(player).sendMessage("summon-too-large");
    verify(world, never()).spawnEntity(any(Location.class), any(EntityType.class));
  }

  @Test
  void aChestThatIsNoLongerThereIsReported() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    when(world.getBlockAt(chestLoc)).thenReturn(block);
    when(block.getType()).thenReturn(Material.STONE);

    assertTrue(run(player, "test-group", "wave1", "random"));

    verify(player).sendMessage("chest-missing");
  }

  @Test
  void aChestWithNoSpawnEggsIsReported() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    givenChestHolding(new ItemStack(Material.DIAMOND, 1));

    assertTrue(run(player, "test-group", "wave1", "random"));

    verify(player).sendMessage("no-spawn-eggs");
  }

  @Test
  void aPlayerWithoutThePermissionIsRefused() {
    when(player.hasPermission("mobclash.summon")).thenReturn(false);

    assertTrue(run(player, "test-group", "wave1", "random"));

    verify(player).sendMessage("no-permission");
    verify(spawnManager, never()).hasGroup(anyString());
  }
}
