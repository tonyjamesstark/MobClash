package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * getMessage is stubbed to echo its key and its substitutions, so an assertion names both the
 * branch that fired and the point it reported.
 */
@ExtendWith(MockitoExtension.class)
class RemoveSpawnCommandTest {

  @Mock private MobClashPlugin plugin;
  @Mock private SpawnManager spawnManager;
  @Mock private LanguageManager langManager;
  @Mock private Player player;
  @Mock private ConsoleCommandSender console;
  @Mock private BlockCommandSender commandBlock;
  @Mock private Block commandBlockBlock;
  @Mock private Command command;
  @Mock private World world;

  private RemoveSpawnCommand removeSpawnCommand;
  private Location point1;
  private Location point2;

  @BeforeEach
  void setUp() {
    removeSpawnCommand = new RemoveSpawnCommand(plugin, spawnManager, langManager);
    point1 = new Location(world, 100.4, 64, 100.6);
    point2 = new Location(world, 200, 65, -200);

    lenient().when(player.hasPermission("mobclash.removespawn")).thenReturn(true);
    lenient().when(world.getName()).thenReturn("world");
    lenient()
        .when(langManager.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              return args[0] + Arrays.toString(Arrays.copyOfRange(args, 1, args.length));
            });
  }

  private boolean run(CommandSender sender, String... args) {
    return removeSpawnCommand.onCommand(sender, command, "removespawn", args);
  }

  private void givenArenaWithTwoPoints() {
    when(spawnManager.hasGroup("arena")).thenReturn(true);
    when(spawnManager.getSpawnPoints("arena")).thenReturn(List.of(point1, point2));
  }

  @Test
  void withoutANumberThePointNearestThePlayerIsRemoved() {
    givenArenaWithTwoPoints();
    Location standing = new Location(world, 101, 64, 101);
    when(player.getLocation()).thenReturn(standing);
    when(spawnManager.removeNearestSpawnPoint("arena", standing)).thenReturn(true);

    assertTrue(run(player, "arena"));

    verify(spawnManager).removeNearestSpawnPoint("arena", standing);
    verify(player).sendMessage("removespawn-success[arena]");
  }

  @Test
  void withoutANumberTheConsoleHasNoPositionToMeasureFrom() {
    givenArenaWithTwoPoints();

    assertTrue(run(console, "arena"));

    verify(console).sendMessage("no-location[]");
    verify(spawnManager, never()).removeNearestSpawnPoint(anyString(), any());
  }

  @Test
  void aNumberRemovesThatPointEvenFromTheConsole() {
    givenArenaWithTwoPoints();
    when(spawnManager.removeSpawnPoint("arena", 1)).thenReturn(point2);

    assertTrue(run(console, "arena", "2"));

    verify(spawnManager).removeSpawnPoint("arena", 1);
    verify(console).sendMessage("removespawn-number-success[arena, 2, world, 200, 65, -200]");
    verify(spawnManager, never()).removeNearestSpawnPoint(anyString(), any());
  }

  @Test
  void aPointWhoseWorldWasUnloadedIsStillRemovedAndSaysSo() {
    givenArenaWithTwoPoints();
    Location inUnloadedWorld = mock(Location.class);
    when(inUnloadedWorld.getWorld()).thenThrow(new IllegalArgumentException("World unloaded"));
    when(inUnloadedWorld.getX()).thenReturn(200.0);
    when(inUnloadedWorld.getY()).thenReturn(65.0);
    when(inUnloadedWorld.getZ()).thenReturn(-200.0);
    when(spawnManager.removeSpawnPoint("arena", 1)).thenReturn(inUnloadedWorld);

    assertTrue(run(console, "arena", "2"));

    verify(spawnManager).removeSpawnPoint("arena", 1);
    verify(console)
        .sendMessage("removespawn-number-success[arena, 2, world-unloaded[], 200, 65, -200]");
  }

  @Test
  void aNumberFromACommandBlockIsLoggedToTheConsole() {
    givenArenaWithTwoPoints();
    when(commandBlock.getBlock()).thenReturn(commandBlockBlock);
    when(commandBlockBlock.getLocation()).thenReturn(new Location(world, 1, 2, 3));
    when(spawnManager.removeSpawnPoint("arena", 0)).thenReturn(point1);

    assertTrue(run(commandBlock, "arena", "1"));

    verify(plugin)
        .log(
            Level.INFO,
            "Command block at world 1 2 3: removespawn-number-success[arena, 1, world, 100, 64,"
                + " 101]");
  }

  @Test
  void aNumberPastTheLastPointChangesNothingAndNamesTheRange() {
    givenArenaWithTwoPoints();

    assertTrue(run(player, "arena", "3"));

    verify(player).sendMessage("invalid-spawn-number[arena, 3, 2]");
    verify(spawnManager, never()).removeSpawnPoint(anyString(), anyInt());
  }

  @Test
  void zeroIsNotASpawnPointNumber() {
    givenArenaWithTwoPoints();

    assertTrue(run(player, "arena", "0"));

    verify(player).sendMessage("invalid-spawn-number[arena, 0, 2]");
    verify(spawnManager, never()).removeSpawnPoint(anyString(), anyInt());
  }

  @Test
  void aNonNumericNumberChangesNothingAndNamesTheRange() {
    givenArenaWithTwoPoints();

    assertTrue(run(player, "arena", "two"));

    verify(player).sendMessage("invalid-spawn-number[arena, two, 2]");
    verify(spawnManager, never()).removeSpawnPoint(anyString(), anyInt());
  }

  @Test
  void anUnknownGroupIsReported() {
    when(spawnManager.hasGroup("ghost")).thenReturn(false);

    assertTrue(run(player, "ghost", "1"));

    verify(player).sendMessage("group-not-exist[ghost]");
    verify(spawnManager, never()).removeSpawnPoint(anyString(), anyInt());
  }

  @Test
  void tooManyArgumentsPrintTheUsage() {
    removeSpawnCommand.setUsage("/<command> <group> [number] - remove a point");

    assertTrue(run(player, "arena", "1", "extra"));

    verify(player).sendMessage("§e/removespawn <group> [number] §7- remove a point");
    verify(spawnManager, never()).hasGroup(anyString());
  }
}
