package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
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

  /**
   * A stack of one material. Constructing a real ItemStack resolves its ItemType through Registry,
   * which needs a running server as of 1.21; the command reads only type, amount and item meta.
   */
  private ItemStack itemOf(Material material, int amount) {
    ItemStack item = mock(ItemStack.class);
    when(item.getType()).thenReturn(material);
    lenient().when(item.getAmount()).thenReturn(amount);
    return item;
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
  void randomModeSpreadsTheRequestedCountOverThePoints() {
    Location spawnLoc2 = new Location(world, 200, 64, 200);
    givenGroup("test-group", "wave1", List.of(spawnLoc, spawnLoc2));
    givenCapOf(500);
    givenChestHolding(itemOf(Material.ZOMBIE_SPAWN_EGG, 5));
    givenSpawnOf(EntityType.ZOMBIE, true);

    assertTrue(run(player, "test-group", "wave1", "random", "20"));

    verify(world, times(20)).spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
    verify(world, atLeastOnce()).spawnEntity(eq(spawnLoc), eq(EntityType.ZOMBIE));
    verify(world, atLeastOnce()).spawnEntity(eq(spawnLoc2), eq(EntityType.ZOMBIE));
    verify(mobTracker, times(20)).tagMob(any(), eq("test-group"), eq("wave1"));
    verify(player).sendMessage("summonmobs-success");
  }

  @Test
  void allModeSpawnsTheRequestedCountAtEveryPoint() {
    Location spawnLoc2 = new Location(world, 200, 64, 200);
    givenGroup("test-group", "wave1", List.of(spawnLoc, spawnLoc2));
    givenCapOf(500);
    givenChestHolding(itemOf(Material.ZOMBIE_SPAWN_EGG, 5));
    givenSpawnOf(EntityType.ZOMBIE, true);

    assertTrue(run(player, "test-group", "wave1", "all", "2"));

    verify(world, times(4)).spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
    verify(mobTracker, times(4)).tagMob(any(), eq("test-group"), eq("wave1"));
  }

  /** An egg whose item meta carries entity_data, as getSpawnedEntity reports it. */
  private EntitySnapshot givenDataEgg(EntityType type, String snbt, int amount) {
    EntitySnapshot data = mock(EntitySnapshot.class);
    when(data.getEntityType()).thenReturn(type);
    lenient().when(data.getAsString()).thenReturn(snbt);
    SpawnEggMeta meta = mock(SpawnEggMeta.class);
    when(meta.getSpawnedEntity()).thenReturn(data);
    ItemStack egg = itemOf(Material.ZOMBIE_SPAWN_EGG, amount);
    when(egg.getItemMeta()).thenReturn(meta);
    givenChestHolding(egg);
    return data;
  }

  /** A mob from a data egg, accepted or refused by the server. */
  private Mob givenDataSpawn(EntitySnapshot data, boolean accepted) {
    Mob mob = mock(Mob.class);
    when(mob.isValid()).thenReturn(accepted);
    lenient().when(mob.getEquipment()).thenReturn(mock(EntityEquipment.class));
    when(data.createEntity(any(Location.class))).thenReturn(mob);
    when(spawnManager.getRandom()).thenReturn(new Random(42));
    when(plugin.getMobTracker()).thenReturn(mobTracker);
    return mob;
  }

  @Test
  void anEggsEntityDataIsKeptOnTheSpawnedMob() {
    // The swan_farms Monster Mash eggs carry entity_data with attributes, Health, DeathLootTable
    // and PersistenceRequired. Spawning by type alone produced a vanilla zombie without any of it.
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    EntitySnapshot data =
        givenDataEgg(EntityType.ZOMBIE, "{id:\"minecraft:zombie\",PersistenceRequired:1b}", 2);
    Mob mob = givenDataSpawn(data, true);

    assertTrue(run(player, "test-group", "wave1", "all", "3"));

    verify(data, times(3)).createEntity(spawnLoc);
    verify(world, never()).spawnEntity(any(Location.class), any(EntityType.class));
    verify(mobTracker, times(3)).tagMob(mob, "test-group", "wave1");
  }

  @Test
  void gearFromAnEggNeverDrops() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    EntitySnapshot data =
        givenDataEgg(
            EntityType.ZOMBIE,
            "{id:\"minecraft:zombie\",equipment:{mainhand:{id:\"minecraft:iron_sword\"}}}",
            1);
    Mob mob = givenDataSpawn(data, true);

    run(player, "test-group", "wave1", "all", "1");

    for (EquipmentSlot slot : EquipmentSlot.values()) {
      verify(mob.getEquipment()).setDropChance(slot, 0f);
    }
  }

  @Test
  void aRefusedSpawnFromADataEggIsNotTagged() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    EntitySnapshot data = givenDataEgg(EntityType.ZOMBIE, "{id:\"minecraft:zombie\"}", 1);
    givenDataSpawn(data, false);

    run(player, "test-group", "wave1", "random", "3");

    verify(mobTracker, never()).tagMob(any(), anyString(), anyString());
  }

  @Test
  void aDataEggThatIsNotAMobIsSkippedWithAWarning() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    EntitySnapshot data =
        givenDataEgg(EntityType.FALLING_BLOCK, "{id:\"minecraft:falling_block\"}", 1);

    assertTrue(run(player, "test-group", "wave1", "random"));

    verify(player).sendMessage("egg-not-a-mob");
    verify(player).sendMessage("no-spawn-eggs");
    verify(data, never()).createEntity(any(Location.class));
  }

  @Test
  void aDataEggWithRidersIsSkippedWithAWarning() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    EntitySnapshot data =
        givenDataEgg(
            EntityType.ZOMBIE,
            "{id:\"minecraft:zombie\",Passengers:[{id:\"minecraft:zombie\"}]}",
            1);

    assertTrue(run(player, "test-group", "wave1", "random"));

    verify(player).sendMessage("egg-has-riders");
    verify(data, never()).createEntity(any(Location.class));
  }

  @Test
  void aDataEggWithAFixedUuidIsSkippedWithAWarningToTheCommandBlock() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    EntitySnapshot data =
        givenDataEgg(EntityType.ZOMBIE, "{id:\"minecraft:zombie\",UUID:[I;1,2,3,4]}", 1);

    assertTrue(run(commandBlock, "test-group", "wave1", "random"));

    verify(commandBlock).sendMessage("egg-has-uuid");
    verify(data, never()).createEntity(any(Location.class));
  }

  @Test
  void aCommandBlockMaySummonWithoutPermission() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    givenChestHolding(itemOf(Material.ZOMBIE_SPAWN_EGG, 1));
    givenSpawnOf(EntityType.ZOMBIE, true);

    assertTrue(run(commandBlock, "test-group", "wave1", "random", "1"));

    verify(world).spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
    verify(commandBlock, never()).sendMessage("no-permission");
  }

  @Test
  void anEggWhoseEnumNameDiffersFromItsEntityStillMaps() {
    // This egg's entity constant was MUSHROOM_COW on 1.20 and is MOOSHROOM on 1.21. Matching
    // enum names dropped it silently on 1.20 and would break again at the rename; the key,
    // minecraft:mooshroom, is the same on both.
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    givenChestHolding(itemOf(Material.MOOSHROOM_SPAWN_EGG, 1));
    givenSpawnOf(EntityType.MOOSHROOM, true);

    assertTrue(run(player, "test-group", "wave1", "random", "1"));

    verify(world).spawnEntity(any(Location.class), eq(EntityType.MOOSHROOM));
  }

  @Test
  void nonEggContentsAndEmptySlotsAreIgnored() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    givenChestHolding(null, itemOf(Material.DIAMOND, 64), itemOf(Material.ZOMBIE_SPAWN_EGG, 1));
    givenSpawnOf(EntityType.ZOMBIE, true);

    assertTrue(run(player, "test-group", "wave1", "random", "1"));

    verify(world).spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
  }

  @Test
  void aRefusedSpawnIsNotCountedOrTagged() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    givenChestHolding(itemOf(Material.ZOMBIE_SPAWN_EGG, 5));
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
  void anArgumentThatIsNeitherAnAmountNorEquipmentIsRejected() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    assertTrue(run(player, "test-group", "wave1", "random", "lots"));
    verify(player).sendMessage("invalid-equipment");
    verify(world, never()).spawnEntity(any(Location.class), any(EntityType.class));
  }

  @Test
  void anExtraArgumentPrintsTheUsage() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    assertTrue(run(player, "test-group", "wave1", "random", "1", "false", "more"));
    verify(player).sendMessage("summonmobs-usage");
  }

  @Test
  void equipmentFalseLeavesVanillaGearAlone() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    givenChestHolding(itemOf(Material.ZOMBIE_SPAWN_EGG, 1));
    LivingEntity mob = givenSpawnOf(EntityType.ZOMBIE, true);

    assertTrue(run(player, "test-group", "wave1", "all", "2", "false"));

    verify(world, times(2)).spawnEntity(spawnLoc, EntityType.ZOMBIE);
    verify(mob, never()).getEquipment();
  }

  /** A second chest in the group, set with /setchest test-group gear, holding the given items. */
  private void givenGearChestHolding(ItemStack... contents) {
    Location gearLoc = new Location(world, 60, 64, 60);
    Block gearBlock = mock(Block.class);
    Chest gearChest = mock(Chest.class);
    Inventory gearInventory = mock(Inventory.class);
    when(spawnManager.getGroupChest("test-group", "gear")).thenReturn(gearLoc);
    when(world.getBlockAt(gearLoc)).thenReturn(gearBlock);
    when(gearBlock.getType()).thenReturn(Material.CHEST);
    when(gearBlock.getState()).thenReturn(gearChest);
    when(gearChest.getInventory()).thenReturn(gearInventory);
    when(gearInventory.getContents()).thenReturn(contents);
  }

  @Test
  void aGearChestThatIsNoLongerThereIsReported() {
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    Location gearLoc = new Location(world, 60, 64, 60);
    Block gearBlock = mock(Block.class);
    when(spawnManager.getGroupChest("test-group", "gear")).thenReturn(gearLoc);
    when(world.getBlockAt(gearLoc)).thenReturn(gearBlock);
    when(gearBlock.getType()).thenReturn(Material.STONE);

    assertTrue(run(player, "test-group", "wave1", "random", "gear"));

    verify(player).sendMessage("chest-missing");
    verify(world, never()).spawnEntity(any(Location.class), any(EntityType.class));
  }

  /** A zombie as the world spawns it from a plain egg, with an equipment to watch. */
  private EntityEquipment givenEquippableZombie() {
    Mob mob = mock(Mob.class);
    when(mob.isValid()).thenReturn(true);
    when(mob.getType()).thenReturn(EntityType.ZOMBIE);
    EntityEquipment equipment = mock(EntityEquipment.class);
    when(mob.getEquipment()).thenReturn(equipment);
    when(world.spawnEntity(any(Location.class), eq(EntityType.ZOMBIE))).thenReturn(mob);
    when(spawnManager.getRandom()).thenReturn(new Random(42));
    when(plugin.getMobTracker()).thenReturn(mobTracker);
    when(config.getInt(eq("random-equipment.max-protection"), anyInt())).thenReturn(4);
    when(config.getInt(eq("random-equipment.max-sharpness"), anyInt())).thenReturn(5);
    return equipment;
  }

  @Test
  void equippingAPlainEggMobClearsVanillaGearAndNothingDrops() {
    // An empty gear chest draws nothing, which isolates the clearing from what is drawn.
    givenGroup("test-group", "wave1", List.of(spawnLoc));
    givenCapOf(500);
    givenChestHolding(itemOf(Material.ZOMBIE_SPAWN_EGG, 1));
    givenGearChestHolding();
    EntityEquipment equipment = givenEquippableZombie();

    assertTrue(run(player, "test-group", "wave1", "all", "gear"));

    for (EquipmentSlot slot :
        List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.HAND)) {
      verify(equipment).setItem(slot, null);
    }
    for (EquipmentSlot slot : EquipmentSlot.values()) {
      verify(equipment).setDropChance(slot, 0f);
    }
    verify(mobTracker).tagMob(any(), eq("test-group"), eq("wave1"));
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
    givenChestHolding(itemOf(Material.DIAMOND, 1));

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
