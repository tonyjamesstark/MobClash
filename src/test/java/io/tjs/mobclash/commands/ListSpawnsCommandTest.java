package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * getMessage is stubbed to echo its key and its substitutions, so an assertion names both the
 * branch that fired and the values it formatted -- counting sendMessage calls would pass whether or
 * not the coordinates were right.
 */
@ExtendWith(MockitoExtension.class)
class ListSpawnsCommandTest {

  @Mock private MobClashPlugin plugin;
  @Mock private SpawnManager spawnManager;
  @Mock private LanguageManager langManager;
  @Mock private CommandSender sender;
  @Mock private Command command;
  @Mock private World world;

  private ListSpawnsCommand listSpawnsCommand;

  @BeforeEach
  void setUp() {
    listSpawnsCommand = new ListSpawnsCommand(plugin, spawnManager, langManager);

    lenient().when(sender.hasPermission("mobclash.listspawns")).thenReturn(true);
    lenient()
        .when(langManager.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              return args[0] + Arrays.toString(Arrays.copyOfRange(args, 1, args.length));
            });
  }

  private boolean run(String... args) {
    return listSpawnsCommand.onCommand(sender, command, "listspawns", args);
  }

  @Test
  void eachSpawnPointIsListedInOrderWithRoundedCoordinates() {
    when(world.getName()).thenReturn("world");
    when(spawnManager.hasGroup("test-group")).thenReturn(true);
    when(spawnManager.getSpawnPoints("test-group"))
        .thenReturn(
            List.of(new Location(world, 100.4, 64, 100.6), new Location(world, -200.5, 71, 12)));

    assertTrue(run("test-group"));

    InOrder order = inOrder(sender);
    order.verify(sender).sendMessage("listspawns-header[test-group, 2]");
    order.verify(sender).sendMessage("listspawns-entry[1, world, 100, 64, 101]");
    order.verify(sender).sendMessage("listspawns-entry[2, world, -200, 71, 12]");
    order.verifyNoMoreInteractions();
  }

  @Test
  void noArgumentsPrintsTheUsage() {
    assertTrue(run());
    verify(sender).sendMessage("listspawns-usage[]");
  }

  @Test
  void aMissingGroupIsReportedWithItsName() {
    when(spawnManager.hasGroup("nonexistent")).thenReturn(false);

    assertTrue(run("nonexistent"));

    verify(sender).sendMessage("group-not-exist[nonexistent]");
  }

  @Test
  void anEmptyGroupIsReportedWithItsName() {
    when(spawnManager.hasGroup("empty-group")).thenReturn(true);
    when(spawnManager.getSpawnPoints("empty-group")).thenReturn(List.of());

    assertTrue(run("empty-group"));

    verify(sender).sendMessage("group-no-points[empty-group]");
  }

  @Test
  void aSenderWithoutThePermissionIsRefused() {
    when(sender.hasPermission("mobclash.listspawns")).thenReturn(false);

    assertTrue(run("test-group"));

    verify(sender).sendMessage("no-permission[]");
    verify(spawnManager, never()).hasGroup(anyString());
  }
}
