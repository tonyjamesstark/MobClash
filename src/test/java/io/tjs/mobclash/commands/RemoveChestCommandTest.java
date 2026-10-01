package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.Arrays;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * getMessage is stubbed to echo its key and its substitutions, so an assertion names both the
 * branch that fired and the group and chest it named.
 */
@ExtendWith(MockitoExtension.class)
class RemoveChestCommandTest {

  @Mock private MobClashPlugin plugin;
  @Mock private SpawnManager spawnManager;
  @Mock private LanguageManager langManager;
  @Mock private CommandSender sender;
  @Mock private BlockCommandSender commandBlock;
  @Mock private Block commandBlockBlock;
  @Mock private World world;
  @Mock private Command command;

  private RemoveChestCommand removeChestCommand;

  @BeforeEach
  void setUp() {
    removeChestCommand = new RemoveChestCommand(plugin, spawnManager, langManager);

    lenient().when(sender.hasPermission("mobclash.removechest")).thenReturn(true);
    lenient()
        .when(langManager.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              return args[0] + Arrays.toString(Arrays.copyOfRange(args, 1, args.length));
            });
  }

  private boolean run(CommandSender who, String... args) {
    return removeChestCommand.onCommand(who, command, "removechest", args);
  }

  @Test
  void aSetChestIsForgotten() {
    when(spawnManager.removeGroupChest("arena", "wave1")).thenReturn(true);

    assertTrue(run(sender, "arena", "wave1"));

    verify(spawnManager).removeGroupChest("arena", "wave1");
    verify(sender).sendMessage("removechest-success[arena, wave1]");
  }

  @Test
  void aChestThatIsNotSetIsReported() {
    when(spawnManager.removeGroupChest("arena", "wave9")).thenReturn(false);

    assertTrue(run(sender, "arena", "wave9"));

    verify(sender).sendMessage("chest-not-set[arena, wave9]");
  }

  @Test
  void aCommandBlockMayRemoveAndItsReplyReachesTheConsole() {
    when(commandBlock.getBlock()).thenReturn(commandBlockBlock);
    when(commandBlockBlock.getLocation()).thenReturn(new Location(world, 1, 2, 3));
    when(world.getName()).thenReturn("world");
    when(spawnManager.removeGroupChest("arena", "wave1")).thenReturn(true);

    assertTrue(run(commandBlock, "arena", "wave1"));

    verify(commandBlock, never()).hasPermission(anyString());
    verify(plugin)
        .log(Level.INFO, "Command block at world 1 2 3: removechest-success[arena, wave1]");
  }

  @Test
  void theWrongNumberOfArgumentsPrintsTheUsage() {
    removeChestCommand.setUsage("/<command> <group> <chest> - forget a chest");

    assertTrue(run(sender, "arena"));
    assertTrue(run(sender, "arena", "wave1", "extra"));

    verify(sender, times(2)).sendMessage("§e/removechest <group> <chest> §7- forget a chest");
    verify(spawnManager, never()).removeGroupChest(anyString(), anyString());
  }

  @Test
  void aSenderWithoutThePermissionIsRefused() {
    when(sender.hasPermission("mobclash.removechest")).thenReturn(false);

    assertTrue(run(sender, "arena", "wave1"));

    verify(sender).sendMessage("no-permission[]");
    verify(spawnManager, never()).removeGroupChest(anyString(), anyString());
  }
}
