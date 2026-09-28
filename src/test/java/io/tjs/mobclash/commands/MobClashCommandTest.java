package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobClashCommandTest {

  @Mock private MobClashPlugin plugin;
  @Mock private LanguageManager langManager;
  @Mock private CommandSender sender;
  @Mock private Command command;

  private final List<String[]> killsRan = new ArrayList<>();
  private MobClashCommand mobClashCommand;

  @BeforeEach
  void setUp() {
    Map<String, BaseCommand> subcommands = new LinkedHashMap<>();
    subcommands.put("kills", recording("mobclash.kills", killsRan));
    subcommands.put("reload", recording("mobclash.reload", new ArrayList<>()));
    mobClashCommand = new MobClashCommand(langManager, subcommands);
  }

  private BaseCommand recording(String permission, List<String[]> ran) {
    return new BaseCommand(plugin, null, langManager, permission, false) {
      @Override
      protected boolean execute(CommandSender sender, String[] args) {
        ran.add(args);
        return true;
      }
    };
  }

  private void mayRunKillsButNotReload() {
    when(sender.hasPermission("mobclash.kills")).thenReturn(true);
    when(sender.hasPermission("mobclash.reload")).thenReturn(false);
  }

  @Test
  void helpListsOnlyTheCommandsTheSenderMayRun() {
    mayRunKillsButNotReload();
    when(langManager.getMessage("help-header")).thenReturn("header");
    when(langManager.getMessage("help-kills")).thenReturn("kills line");

    for (String[] args : new String[][] {{}, {"help"}, {"HELP"}}) {
      assertTrue(mobClashCommand.onCommand(sender, command, "mobclash", args));
    }

    InOrder order = inOrder(sender);
    for (int i = 0; i < 3; i++) {
      order.verify(sender).sendMessage("header");
      order.verify(sender).sendMessage("kills line");
    }
    verify(langManager, never()).getMessage("help-reload");
  }

  @Test
  void aSubcommandRunsWithTheArgumentsAfterItsName() {
    when(sender.hasPermission("mobclash.kills")).thenReturn(true);

    assertTrue(mobClashCommand.onCommand(sender, command, "mobclash", new String[] {"kills"}));
    assertTrue(
        mobClashCommand.onCommand(sender, command, "mobclash", new String[] {"KILLS", "top", "5"}));

    assertEquals(2, killsRan.size());
    assertArrayEquals(new String[0], killsRan.get(0));
    assertArrayEquals(new String[] {"top", "5"}, killsRan.get(1));
  }

  @Test
  void aSubcommandKeepsItsOwnPermission() {
    when(sender.hasPermission("mobclash.reload")).thenReturn(false);
    when(langManager.getMessage("no-permission")).thenReturn("denied");

    mobClashCommand.onCommand(sender, command, "mobclash", new String[] {"reload"});

    verify(sender).sendMessage("denied");
  }

  @Test
  void anUnknownSubcommandIsNamedWithAPointerToHelp() {
    when(langManager.getMessage("unknown-subcommand", "kils")).thenReturn("no kils");

    assertTrue(mobClashCommand.onCommand(sender, command, "mobclash", new String[] {"kils"}));

    verify(sender).sendMessage("no kils");
    assertTrue(killsRan.isEmpty());
  }

  @Test
  void tabCompletionOffersHelpAndTheUsableSubcommandsByPrefix() {
    mayRunKillsButNotReload();

    assertEquals(
        List.of("help", "kills"),
        mobClashCommand.onTabComplete(sender, command, "mobclash", new String[] {""}));
    assertEquals(
        List.of("kills"),
        mobClashCommand.onTabComplete(sender, command, "mobclash", new String[] {"K"}));
    assertEquals(
        List.of(), mobClashCommand.onTabComplete(sender, command, "mobclash", new String[] {"re"}));
  }
}
