package com.example.mobclash.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.mobclash.managers.LanguageManager;
import com.example.mobclash.managers.SpawnManager;
import java.util.List;
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

  @Mock private org.bukkit.plugin.java.JavaPlugin plugin;

  @Mock private SpawnManager spawnManager;

  @Mock private LanguageManager langManager;

  @Mock private Player player;

  @Mock private ConsoleCommandSender console;

  @Mock private BlockCommandSender commandBlock;

  @Mock private Command command;

  @Mock private World world;

  private AddSpawnCommand addSpawnCommand;

  @BeforeEach
  void setUp() {
    addSpawnCommand = new AddSpawnCommand(plugin, spawnManager, langManager);
    when(langManager.getMessage(anyString(), any())).thenReturn("Message");
  }

  @Test
  void testPlayerWithPermissionCanAddSpawn() {
    when(player.hasPermission("mobspawner.addspawn")).thenReturn(true);
    Location loc = new Location(world, 100, 64, 100);
    when(player.getLocation()).thenReturn(loc);
    when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of(loc));

    boolean result =
        addSpawnCommand.onCommand(player, command, "addspawn", new String[] {"test-group"});

    assertTrue(result);
    verify(spawnManager).addSpawnPoint("test-group", loc);
  }

  @Test
  void testPlayerWithoutPermissionCannotAddSpawn() {
    when(player.hasPermission("mobspawner.addspawn")).thenReturn(false);
    when(langManager.getMessage("no-permission")).thenReturn("No permission!");

    boolean result =
        addSpawnCommand.onCommand(player, command, "addspawn", new String[] {"test-group"});

    assertTrue(result);
    verify(spawnManager, never()).addSpawnPoint(anyString(), any());
    verify(player).sendMessage("No permission!");
  }

  @Test
  void testCommandBlockBypassesPermission() {
    // Command blocks don't have hasPermission method, but BaseCommand handles this
    Location loc = new Location(world, 100, 64, 100);
    when(player.getLocation()).thenReturn(loc);
    when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of(loc));

    boolean result =
        addSpawnCommand.onCommand(commandBlock, command, "addspawn", new String[] {"test-group"});

    // Command block can't be a player, so this should fail with "players-only"
    assertTrue(result);
    verify(commandBlock).sendMessage(anyString());
  }

  @Test
  void testConsoleCannotExecute() {
    when(langManager.getMessage("players-only")).thenReturn("Players only!");

    boolean result =
        addSpawnCommand.onCommand(console, command, "addspawn", new String[] {"test-group"});

    assertTrue(result);
    verify(console).sendMessage("Players only!");
    verify(spawnManager, never()).addSpawnPoint(anyString(), any());
  }

  @Test
  void testMissingArguments() {
    when(player.hasPermission("mobspawner.addspawn")).thenReturn(true);
    when(langManager.getMessage("addspawn-usage")).thenReturn("Usage: /addspawn <group>");

    boolean result = addSpawnCommand.onCommand(player, command, "addspawn", new String[] {});

    assertTrue(result);
    verify(player).sendMessage("Usage: /addspawn <group>");
    verify(spawnManager, never()).addSpawnPoint(anyString(), any());
  }
}
