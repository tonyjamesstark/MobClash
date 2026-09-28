package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReloadCommandTest {

  @Mock private MobClashPlugin plugin;
  @Mock private SpawnManager spawnManager;
  @Mock private LanguageManager langManager;
  @Mock private CommandSender sender;
  @Mock private Command command;

  private ReloadCommand reloadCommand;

  @BeforeEach
  void setUp() {
    reloadCommand = new ReloadCommand(plugin, spawnManager, langManager);
  }

  @Test
  void reloadsThenConfirmsInTheReloadedLanguage() throws InvalidConfigurationException {
    when(sender.hasPermission("mobclash.reload")).thenReturn(true);
    when(langManager.getMessage("reload-success")).thenReturn("reloaded");

    assertTrue(reloadCommand.onCommand(sender, command, "reload", new String[0]));

    InOrder order = inOrder(plugin, langManager, sender);
    order.verify(plugin).reloadSettings();
    order.verify(langManager).getMessage("reload-success");
    order.verify(sender).sendMessage("reloaded");
  }

  @Test
  void aFileThatDoesNotParseIsNamedAndNotReportedAsReloaded() throws InvalidConfigurationException {
    when(sender.hasPermission("mobclash.reload")).thenReturn(true);
    doThrow(new InvalidConfigurationException("language.yml")).when(plugin).reloadSettings();
    when(plugin.getLogger()).thenReturn(Logger.getLogger("ReloadCommandTest"));
    when(langManager.getMessage("reload-failed", "language.yml"))
        .thenReturn("failed: language.yml");

    assertTrue(reloadCommand.onCommand(sender, command, "reload", new String[0]));

    verify(sender).sendMessage("failed: language.yml");
    verify(langManager, never()).getMessage("reload-success");
  }

  @Test
  void aSenderWithoutThePermissionIsRefused() throws InvalidConfigurationException {
    when(sender.hasPermission("mobclash.reload")).thenReturn(false);
    when(langManager.getMessage("no-permission")).thenReturn("denied");

    assertTrue(reloadCommand.onCommand(sender, command, "reload", new String[0]));

    verify(plugin, never()).reloadSettings();
    verify(sender).sendMessage("denied");
  }
}
