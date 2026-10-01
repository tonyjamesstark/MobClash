package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** getMessage echoes its key, so an assertion names the branch that fired. */
@ExtendWith(MockitoExtension.class)
class SetChestCommandTest {

  @Mock private MobClashPlugin plugin;
  @Mock private SpawnManager spawnManager;
  @Mock private LanguageManager langManager;
  @Mock private Player player;
  @Mock private Command command;

  private SetChestCommand setChestCommand;
  private final Location chestLoc = new Location(null, 50, 64, 50);

  @BeforeEach
  void setUp() {
    setChestCommand = new SetChestCommand(plugin, spawnManager, langManager);
    lenient().when(command.getName()).thenReturn("setchest");
    lenient().when(player.getName()).thenReturn("TestPlayer");
    lenient().when(player.hasPermission("mobclash.setchest")).thenReturn(true);
    lenient()
        .when(langManager.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private boolean run(String... args) {
    return setChestCommand.onCommand(player, command, "setchest", args);
  }

  /**
   * The player looks at a block of this type, with the state the server gives it. The state is
   * lenient because a refused block's is never read: a trapped chest has a Chest state, so a rule
   * that went by the state would accept it and its test would fail.
   */
  private void givenTarget(Material type, Class<? extends Container> state) {
    Block block = mock(Block.class);
    Container container = mock(state);
    when(player.getTargetBlock(null, 5)).thenReturn(block);
    when(block.getType()).thenReturn(type);
    lenient().when(block.getState()).thenReturn(container);
    lenient().when(container.getInventory()).thenReturn(mock(Inventory.class));
    lenient().when(block.getLocation()).thenReturn(chestLoc);
  }

  private void assertAccepted() {
    when(spawnManager.hasGroup("arena")).thenReturn(true);

    assertTrue(run("arena", "wave1"));

    verify(spawnManager).setGroupChest("arena", "wave1", chestLoc);
    verify(player).sendMessage("setchest-success");
  }

  private void assertRefused() {
    when(spawnManager.hasGroup("arena")).thenReturn(true);

    assertTrue(run("arena", "wave1"));

    verify(player).sendMessage("must-look-chest");
    verify(spawnManager, never()).setGroupChest(anyString(), anyString(), any());
  }

  @Test
  void aChestIsAccepted() {
    givenTarget(Material.CHEST, Chest.class);
    assertAccepted();
  }

  @Test
  void aBarrelIsAccepted() {
    givenTarget(Material.BARREL, Barrel.class);
    assertAccepted();
  }

  @Test
  void aShulkerBoxOfAnyColourIsAccepted() {
    givenTarget(Material.LIME_SHULKER_BOX, ShulkerBox.class);
    assertAccepted();
  }

  @Test
  void anUndyedShulkerBoxIsAccepted() {
    givenTarget(Material.SHULKER_BOX, ShulkerBox.class);
    assertAccepted();
  }

  @Test
  void aTrappedChestIsRefused() {
    givenTarget(Material.TRAPPED_CHEST, Chest.class);
    assertRefused();
  }

  @Test
  void aCopperChestIsRefused() {
    givenTarget(Material.WEATHERED_COPPER_CHEST, Chest.class);
    assertRefused();
  }

  @Test
  void aHopperIsRefused() {
    givenTarget(Material.HOPPER, Hopper.class);
    assertRefused();
  }

  @Test
  void anUnknownGroupIsReportedBeforeTheTargetIsRead() {
    when(spawnManager.hasGroup("ghost")).thenReturn(false);

    assertTrue(run("ghost", "wave1"));

    verify(player).sendMessage("group-not-exist");
    verify(player, never()).getTargetBlock(null, 5);
  }
}
