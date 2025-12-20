package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListSpawnsCommandTest {

  @Mock(lenient = true)
  private JavaPlugin plugin;

  @Mock(lenient = true)
  private SpawnManager spawnManager;

  @Mock(lenient = true)
  private LanguageManager langManager;

  @Mock(lenient = true)
  private CommandSender sender;

  @Mock(lenient = true)
  private Command command;

  @Mock(lenient = true)
  private World world;

  private ListSpawnsCommand listSpawnsCommand;

  @BeforeEach
  void setUp() {
    listSpawnsCommand = new ListSpawnsCommand(plugin, spawnManager, langManager);

    when(langManager.getMessage("listspawns-usage")).thenReturn("§cUsage: /listspawns <group>");
    when(langManager.getMessage(eq("group-not-exist"), anyString()))
        .thenReturn("§cGroup doesn't exist!");
    when(langManager.getMessage(eq("group-no-points"), anyString()))
        .thenReturn("§cNo spawn points!");
    when(langManager.getMessage(eq("listspawns-header"), anyString(), anyInt()))
        .thenReturn("§a=== Spawn Points ===");
    when(langManager.getMessage(
            eq("listspawns-entry"), anyInt(), anyString(), anyLong(), anyLong(), anyLong()))
        .thenReturn("§e#1 world (100, 64, 100)");

    when(sender.hasPermission("mobspawner.listspawns")).thenReturn(true);
    when(command.getName()).thenReturn("listspawns");
    when(world.getName()).thenReturn("world");
  }

  @Test
  void testListSpawnsSuccessfully() {
    Location loc1 = new Location(world, 100, 64, 100);
    Location loc2 = new Location(world, 200, 64, 200);

    when(spawnManager.hasGroup("test-group")).thenReturn(true);
    when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of(loc1, loc2));

    boolean result =
        listSpawnsCommand.onCommand(sender, command, "listspawns", new String[] {"test-group"});

    assertTrue(result);
    verify(sender, times(3)).sendMessage(anyString()); // Header + 2 entries
  }

  @Test
  void testListSpawnsMissingArguments() {
    boolean result = listSpawnsCommand.onCommand(sender, command, "listspawns", new String[] {});

    assertTrue(result);
    verify(sender).sendMessage("§cUsage: /listspawns <group>");
  }

  @Test
  void testListSpawnsGroupDoesNotExist() {
    when(spawnManager.hasGroup("nonexistent")).thenReturn(false);

    boolean result =
        listSpawnsCommand.onCommand(sender, command, "listspawns", new String[] {"nonexistent"});

    assertTrue(result);
    verify(sender).sendMessage("§cGroup doesn't exist!");
  }

  @Test
  void testListSpawnsNoPoints() {
    when(spawnManager.hasGroup("empty-group")).thenReturn(true);
    when(spawnManager.getSpawnPoints("empty-group")).thenReturn(List.of());

    boolean result =
        listSpawnsCommand.onCommand(sender, command, "listspawns", new String[] {"empty-group"});

    assertTrue(result);
    verify(sender).sendMessage("§cNo spawn points!");
  }
}
