package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddSpawnCommandTest {

  @Mock private MobClashPlugin plugin;

  @Mock private SpawnManager spawnManager;

  @Mock private LanguageManager langManager;

  @Mock private Player player;

  @Mock private ConsoleCommandSender console;

  @Mock private BlockCommandSender commandBlock;

  @Mock private Command command;

  @Mock private World world;

  @Mock private Block commandBlockBlock;

  private AddSpawnCommand addSpawnCommand;

  @BeforeEach
  void setUp() {
    addSpawnCommand = new AddSpawnCommand(plugin, spawnManager, langManager);

    // getMessage echoes its key, so an assertion names the branch that fired rather than matching
    // any string. Only these framing stubs are lenient: every sender logs its name on the way in,
    // but a given test uses one sender. The rest are strict, so an unused stub reports itself.
    lenient()
        .when(langManager.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(commandBlock.getName()).thenReturn("CommandBlock");
    lenient().when(console.getName()).thenReturn("Console");
    lenient().when(player.getName()).thenReturn("TestPlayer");
    lenient().when(command.getName()).thenReturn("addspawn");
  }

  @Test
  void testAddSpawnSuccessfully() {
    Location loc = new Location(world, 100, 64, 100);
    when(player.getLocation()).thenReturn(loc);
    when(player.hasPermission("mobclash.addspawn")).thenReturn(true);
    when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of(loc));

    boolean result =
        addSpawnCommand.onCommand(player, command, "addspawn", new String[] {"test-group"});

    assertTrue(result);
    verify(spawnManager).addSpawnPoint("test-group", loc);
    verify(player).sendMessage("addspawn-success");
  }

  @Test
  void testAddSpawnNoPermission() {
    when(player.hasPermission("mobclash.addspawn")).thenReturn(false);

    boolean result =
        addSpawnCommand.onCommand(player, command, "addspawn", new String[] {"test-group"});

    assertTrue(result);
    verify(player).sendMessage("no-permission");
    verify(spawnManager, never()).addSpawnPoint(anyString(), any());
  }

  @Test
  void testAddSpawnMissingArguments() {
    when(player.hasPermission("mobclash.addspawn")).thenReturn(true);

    boolean result = addSpawnCommand.onCommand(player, command, "addspawn", new String[] {});

    assertTrue(result);
    verify(player).sendMessage("addspawn-usage");
    verify(spawnManager, never()).addSpawnPoint(anyString(), any());
  }

  @Test
  void testCommandBlockBypassesPermissionAndUsesItsOwnLocation() {
    Location blockLoc = new Location(world, 10, 65, 20);
    when(commandBlock.getBlock()).thenReturn(commandBlockBlock);
    when(commandBlockBlock.getLocation()).thenReturn(blockLoc);
    when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of(blockLoc));

    boolean result =
        addSpawnCommand.onCommand(commandBlock, command, "addspawn", new String[] {"test-group"});

    assertTrue(result);
    // No permission was ever granted to the command block; it ran anyway, at its own position.
    verify(commandBlock, never()).hasPermission(anyString());
    verify(spawnManager).addSpawnPoint("test-group", blockLoc);
    verify(commandBlock).sendMessage("addspawn-success");
  }

  @Test
  void testConsoleHasNoLocationToAdd() {
    boolean result =
        addSpawnCommand.onCommand(console, command, "addspawn", new String[] {"test-group"});

    assertTrue(result);
    verify(console).sendMessage("no-location");
    verify(spawnManager, never()).addSpawnPoint(anyString(), any());
  }

  @Test
  void testDottedGroupNameIsRejected() {
    when(player.hasPermission("mobclash.addspawn")).thenReturn(true);

    boolean result =
        addSpawnCommand.onCommand(player, command, "addspawn", new String[] {"wave.one"});

    assertTrue(result);
    verify(player).sendMessage("invalid-name");
    verify(spawnManager, never()).addSpawnPoint(anyString(), any());
  }
}
