package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * getMessage is stubbed to echo its key and its substitutions, so an assertion names both the
 * branch that fired and the values it formatted.
 */
@ExtendWith(MockitoExtension.class)
class ListChestsCommandTest {

  @Mock private MobClashPlugin plugin;
  @Mock private SpawnManager spawnManager;
  @Mock private LanguageManager langManager;
  @Mock private CommandSender sender;
  @Mock private Command command;
  @Mock private World world;

  private ListChestsCommand listChestsCommand;

  @BeforeEach
  void setUp() {
    listChestsCommand = new ListChestsCommand(plugin, spawnManager, langManager);

    lenient().when(sender.hasPermission("mobclash.listchests")).thenReturn(true);
    lenient()
        .when(langManager.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              return args[0] + Arrays.toString(Arrays.copyOfRange(args, 1, args.length));
            });
  }

  private boolean run(String... args) {
    return listChestsCommand.onCommand(sender, command, "listchests", args);
  }

  /**
   * A stack of one material. Constructing a real ItemStack resolves its ItemType through Registry,
   * which needs a running server; the command reads only type and amount.
   */
  private ItemStack itemOf(Material material, int amount) {
    ItemStack item = mock(ItemStack.class);
    when(item.getType()).thenReturn(material);
    lenient().when(item.getAmount()).thenReturn(amount);
    return item;
  }

  /** The chest block at loc. Location.getBlock() resolves through the world, so stub there. */
  private void givenChestHolding(Location loc, ItemStack... contents) {
    Block block = mock(Block.class);
    Chest chest = mock(Chest.class);
    Inventory inventory = mock(Inventory.class);
    when(world.getBlockAt(loc)).thenReturn(block);
    when(block.getType()).thenReturn(Material.CHEST);
    when(block.getState()).thenReturn(chest);
    when(chest.getInventory()).thenReturn(inventory);
    when(inventory.getContents()).thenReturn(contents);
  }

  private void givenMissingChestAt(Location loc, Material foundType) {
    Block block = mock(Block.class);
    when(world.getBlockAt(loc)).thenReturn(block);
    when(block.getType()).thenReturn(foundType);
  }

  @Test
  void aPresentChestReportsItsEggAndItemCounts() {
    Location loc = new Location(world, 100.4, 64, 100.6);
    givenChestHolding(
        loc,
        itemOf(Material.ZOMBIE_SPAWN_EGG, 5),
        itemOf(Material.SKELETON_SPAWN_EGG, 3),
        itemOf(Material.DIAMOND, 10),
        null);
    when(world.getName()).thenReturn("world");
    when(spawnManager.getChestGroups()).thenReturn(Set.of("group1"));
    when(spawnManager.getGroupWaves("group1")).thenReturn(Map.of("wave1", loc));

    assertTrue(run());

    InOrder order = inOrder(sender);
    order.verify(sender).sendMessage("listchests-header[1]");
    order.verify(sender).sendMessage("listchests-entry[group1, wave1, world, 100, 64, 101, 8, 18]");
    order.verifyNoMoreInteractions();
  }

  @Test
  void aMissingChestNamesTheBlockFound() {
    Location loc = new Location(world, 10, 64, 10);
    givenMissingChestAt(loc, Material.STONE);
    when(world.getName()).thenReturn("world");
    when(spawnManager.getChestGroups()).thenReturn(Set.of("group1"));
    when(spawnManager.getGroupWaves("group1")).thenReturn(Map.of("wave1", loc));

    assertTrue(run());

    verify(sender).sendMessage("listchests-missing[group1, wave1, world, 10, 64, 10, STONE]");
  }

  @Test
  void aChestWhoseWorldWasUnloadedIsListedAsSuch() {
    Location loc = mock(Location.class);
    when(loc.getWorld()).thenThrow(new IllegalArgumentException("World unloaded"));
    when(loc.getX()).thenReturn(10.0);
    when(loc.getY()).thenReturn(64.0);
    when(loc.getZ()).thenReturn(-10.0);
    when(spawnManager.getChestGroups()).thenReturn(Set.of("group1"));
    when(spawnManager.getGroupWaves("group1")).thenReturn(Map.of("wave1", loc));

    assertTrue(run());

    verify(sender).sendMessage("listchests-unloaded[group1, wave1, 10, 64, -10]");
    verify(loc, never()).getBlock();
  }

  @Test
  void aGroupFilterShowsOnlyThatGroupsChests() {
    Location loc = new Location(world, 2, 64, 2);
    givenMissingChestAt(loc, Material.STONE);
    when(world.getName()).thenReturn("world");
    when(spawnManager.hasGroup("group2")).thenReturn(true);
    when(spawnManager.getGroupWaves("group2")).thenReturn(Map.of("wave2", loc));

    assertTrue(run("group2"));

    verify(sender).sendMessage("listchests-header[1]");
    verify(sender).sendMessage("listchests-missing[group2, wave2, world, 2, 64, 2, STONE]");
    verify(spawnManager, never()).getGroupWaves("group1");
    verify(spawnManager, never()).getChestGroups();
  }

  @Test
  void anUnknownGroupIsReportedEvenWhenOnlyChestGroupsExist() {
    when(spawnManager.hasGroup("ghost")).thenReturn(false);
    when(spawnManager.getChestGroups()).thenReturn(Set.of("group1"));

    assertTrue(run("ghost"));

    verify(sender).sendMessage("group-not-exist[ghost]");
  }

  @Test
  void noChestsAtAllGivesTheNoChestsMessage() {
    when(spawnManager.getChestGroups()).thenReturn(Set.of());

    assertTrue(run());

    verify(sender).sendMessage("no-chests[]");
  }

  @Test
  void outputIsSortedByGroupThenChestName() {
    Location alphaWave1 = new Location(world, 1, 64, 1);
    Location alphaWave2 = new Location(world, 2, 64, 2);
    Location betaWave1 = new Location(world, 3, 64, 3);
    givenMissingChestAt(alphaWave1, Material.STONE);
    givenMissingChestAt(alphaWave2, Material.STONE);
    givenMissingChestAt(betaWave1, Material.STONE);
    when(world.getName()).thenReturn("world");
    when(spawnManager.getChestGroups()).thenReturn(Set.of("beta", "alpha"));
    when(spawnManager.getGroupWaves("alpha"))
        .thenReturn(Map.of("wave2", alphaWave2, "wave1", alphaWave1));
    when(spawnManager.getGroupWaves("beta")).thenReturn(Map.of("wave1", betaWave1));

    assertTrue(run());

    InOrder order = inOrder(sender);
    order.verify(sender).sendMessage("listchests-header[3]");
    order.verify(sender).sendMessage("listchests-missing[alpha, wave1, world, 1, 64, 1, STONE]");
    order.verify(sender).sendMessage("listchests-missing[alpha, wave2, world, 2, 64, 2, STONE]");
    order.verify(sender).sendMessage("listchests-missing[beta, wave1, world, 3, 64, 3, STONE]");
  }

  @Test
  void extraArgumentsPrintTheUsage() {
    listChestsCommand.setUsage("/<command> [group] - list chests");
    assertTrue(run("group1", "extra"));

    verify(sender).sendMessage("§e/listchests [group] §7- list chests");
    verify(spawnManager, never()).hasGroup(anyString());
  }

  @Test
  void aSenderWithoutThePermissionIsRefused() {
    when(sender.hasPermission("mobclash.listchests")).thenReturn(false);

    assertTrue(run());

    verify(sender).sendMessage("no-permission[]");
    verify(spawnManager, never()).getChestGroups();
  }
}
