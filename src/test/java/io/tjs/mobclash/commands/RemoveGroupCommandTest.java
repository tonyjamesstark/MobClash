package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * getMessage is stubbed to echo its key and its substitutions, so an assertion names both the
 * branch that fired and the counts it reported.
 */
@ExtendWith(MockitoExtension.class)
class RemoveGroupCommandTest {

  @Mock private MobClashPlugin plugin;
  @Mock private SpawnManager spawnManager;
  @Mock private LanguageManager langManager;
  @Mock private CommandSender sender;
  @Mock private Command command;
  @Mock private World world;

  private RemoveGroupCommand removeGroupCommand;

  @BeforeEach
  void setUp() {
    removeGroupCommand = new RemoveGroupCommand(plugin, spawnManager, langManager);

    lenient().when(sender.hasPermission("mobclash.removegroup")).thenReturn(true);
    lenient()
        .when(langManager.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              return args[0] + Arrays.toString(Arrays.copyOfRange(args, 1, args.length));
            });
  }

  private boolean run(String... args) {
    return removeGroupCommand.onCommand(sender, command, "removegroup", args);
  }

  /** Group arena: two spawn points and one chest. */
  private void givenArena() {
    when(spawnManager.hasGroup("arena")).thenReturn(true);
    when(spawnManager.getSpawnPoints("arena"))
        .thenReturn(List.of(new Location(world, 1, 64, 1), new Location(world, 2, 64, 2)));
    when(spawnManager.getGroupWaves("arena"))
        .thenReturn(Map.of("wave1", new Location(world, 5, 64, 5)));
  }

  @Test
  void withoutConfirmItSaysWhatItWouldRemoveAndChangesNothing() {
    givenArena();

    assertTrue(run("arena"));

    verify(sender).sendMessage("removegroup-confirm[arena, 2, 1]");
    verify(spawnManager, never()).removeGroup(anyString());
  }

  @Test
  void withConfirmItRemovesTheGroupAndSaysWhatWentWithIt() {
    givenArena();

    assertTrue(run("arena", "confirm"));

    verify(spawnManager).removeGroup("arena");
    verify(sender).sendMessage("removegroup-success[arena, 2, 1]");
  }

  @Test
  void confirmIsCaseInsensitive() {
    givenArena();

    assertTrue(run("arena", "CONFIRM"));

    verify(spawnManager).removeGroup("arena");
  }

  @Test
  void aGroupWithOnlyChestsCountsAsExisting() {
    when(spawnManager.hasGroup("arena")).thenReturn(false);
    when(spawnManager.getSpawnPoints("arena")).thenReturn(List.of());
    when(spawnManager.getGroupWaves("arena"))
        .thenReturn(Map.of("wave1", new Location(world, 5, 64, 5)));

    assertTrue(run("arena", "confirm"));

    verify(spawnManager).removeGroup("arena");
    verify(sender).sendMessage("removegroup-success[arena, 0, 1]");
  }

  @Test
  void anUnknownGroupIsReported() {
    when(spawnManager.hasGroup("ghost")).thenReturn(false);
    when(spawnManager.getSpawnPoints("ghost")).thenReturn(List.of());
    when(spawnManager.getGroupWaves("ghost")).thenReturn(Map.of());

    assertTrue(run("ghost", "confirm"));

    verify(sender).sendMessage("group-not-exist[ghost]");
    verify(spawnManager, never()).removeGroup(anyString());
  }

  @Test
  void aSecondArgumentOtherThanConfirmPrintsTheUsageAndChangesNothing() {
    removeGroupCommand.setUsage("/<command> <group> confirm - remove it");

    assertTrue(run("arena", "yes"));

    verify(sender).sendMessage("§e/removegroup <group> confirm §7- remove it");
    verify(spawnManager, never()).removeGroup(anyString());
  }

  @Test
  void aSenderWithoutThePermissionIsRefused() {
    when(sender.hasPermission("mobclash.removegroup")).thenReturn(false);

    assertTrue(run("arena", "confirm"));

    verify(sender).sendMessage("no-permission[]");
    verify(spawnManager, never()).removeGroup(anyString());
  }
}
