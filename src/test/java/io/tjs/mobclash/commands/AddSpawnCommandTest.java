package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.World;
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

  @Mock(lenient = true)
  private MobClashPlugin plugin;

  @Mock(lenient = true)
  private SpawnManager spawnManager;

  @Mock(lenient = true)
  private LanguageManager langManager;

  @Mock(lenient = true)
  private Player player;

  @Mock(lenient = true)
  private ConsoleCommandSender console;

  @Mock(lenient = true)
  private BlockCommandSender commandBlock;

  @Mock(lenient = true)
  private Command command;

  @Mock(lenient = true)
  private World world;

  private AddSpawnCommand addSpawnCommand;

  @BeforeEach
  void setUp() {
    addSpawnCommand = new AddSpawnCommand(plugin, spawnManager, langManager);

    // Properly stub all language manager messages
    when(langManager.getMessage("no-permission")).thenReturn("§cNo permission!");
    when(langManager.getMessage("players-only")).thenReturn("§cPlayers only!");
    when(langManager.getMessage("addspawn-usage")).thenReturn("§cUsage: /addspawn <group>");
    when(langManager.getMessage(eq("addspawn-success"), anyString(), anyInt()))
        .thenReturn("§aSpawn added!");

    // Mock the log method to prevent NPE
    doNothing().when(plugin).log(any(Level.class), anyString());

    // Mock command block and console getName() to prevent NPE
    when(commandBlock.getName()).thenReturn("CommandBlock");
    when(console.getName()).thenReturn("Console");
    when(player.getName()).thenReturn("TestPlayer");
    when(command.getName()).thenReturn("addspawn");
  }

  @Test
  void testAddSpawnSuccessfully() {
    Location loc = new Location(world, 100, 64, 100);
    when(player.getLocation()).thenReturn(loc);
    when(player.hasPermission("mobspawner.addspawn")).thenReturn(true);
    when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of(loc));

    boolean result =
        addSpawnCommand.onCommand(player, command, "addspawn", new String[] {"test-group"});

    assertTrue(result);
    verify(spawnManager).addSpawnPoint("test-group", loc);
    verify(player).sendMessage("§aSpawn added!");
  }

  @Test
  void testAddSpawnNoPermission() {
    when(player.hasPermission("mobspawner.addspawn")).thenReturn(false);

    boolean result =
        addSpawnCommand.onCommand(player, command, "addspawn", new String[] {"test-group"});

    assertTrue(result);
    verify(player).sendMessage("§cNo permission!");
    verify(spawnManager, never()).addSpawnPoint(anyString(), any());
  }

  @Test
  void testAddSpawnMissingArguments() {
    when(player.hasPermission("mobspawner.addspawn")).thenReturn(true);

    boolean result = addSpawnCommand.onCommand(player, command, "addspawn", new String[] {});

    assertTrue(result);
    verify(player).sendMessage("§cUsage: /addspawn <group>");
    verify(spawnManager, never()).addSpawnPoint(anyString(), any());
  }

  @Test
  void testCommandBlockBypassesPermission() {
    // Command blocks can't be players, so this should fail with "players-only"
    boolean result =
        addSpawnCommand.onCommand(commandBlock, command, "addspawn", new String[] {"test-group"});

    assertTrue(result);
    verify(commandBlock).sendMessage("§cPlayers only!");
  }

  @Test
  void testConsoleCannotExecute() {
    // Console needs permission to get past the permission check
    when(console.hasPermission("mobspawner.addspawn")).thenReturn(true);

    boolean result =
        addSpawnCommand.onCommand(console, command, "addspawn", new String[] {"test-group"});

    assertTrue(result);
    verify(console).sendMessage("§cPlayers only!");
    verify(spawnManager, never()).addSpawnPoint(anyString(), any());
  }
}
