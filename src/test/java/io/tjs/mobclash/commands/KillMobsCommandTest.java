package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.Arrays;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * getMessage is stubbed to echo its key and its substitutions, so an assertion names both the
 * branch that fired and the count it reported. Which mobs go is MobTrackerTest's concern.
 */
@ExtendWith(MockitoExtension.class)
class KillMobsCommandTest {

  @Mock private MobClashPlugin plugin;
  @Mock private SpawnManager spawnManager;
  @Mock private LanguageManager langManager;
  @Mock private MobTracker mobTracker;
  @Mock private CommandSender sender;
  @Mock private ConsoleCommandSender console;
  @Mock private Command command;

  private KillMobsCommand killMobsCommand;

  @BeforeEach
  void setUp() {
    killMobsCommand = new KillMobsCommand(plugin, spawnManager, langManager, mobTracker);

    lenient().when(sender.hasPermission("mobclash.killmobs")).thenReturn(true);
    lenient()
        .when(langManager.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(
            invocation -> {
              Object[] args = invocation.getArguments();
              return args[0] + Arrays.toString(Arrays.copyOfRange(args, 1, args.length));
            });
  }

  private boolean run(CommandSender who, String... args) {
    return killMobsCommand.onCommand(who, command, "killmobs", args);
  }

  @Test
  void withoutAGroupEveryGroupsMobsGoAndTheCountIsReported() {
    when(mobTracker.removeMobs(null)).thenReturn(7);

    assertTrue(run(sender));

    verify(sender).sendMessage("killmobs-success[7]");
  }

  @Test
  void aGroupRemovesOnlyItsMobsAndNamesItWithTheCount() {
    when(mobTracker.removeMobs("arena")).thenReturn(3);

    assertTrue(run(console, "arena"));

    verify(console).sendMessage("killmobs-group-success[arena, 3]");
  }

  @Test
  void anExtraArgumentPrintsTheUsageAndRemovesNothing() {
    killMobsCommand.setUsage("/<command> [group] - remove MobClash mobs");

    assertTrue(run(sender, "arena", "extra"));

    verify(sender).sendMessage("§e/killmobs [group] §7- remove MobClash mobs");
    verifyNoInteractions(mobTracker);
  }

  @Test
  void aSenderWithoutThePermissionIsRefused() {
    when(sender.hasPermission("mobclash.killmobs")).thenReturn(false);

    assertTrue(run(sender));

    verify(sender).sendMessage("no-permission[]");
    verifyNoInteractions(mobTracker);
  }
}
