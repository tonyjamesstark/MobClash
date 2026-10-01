package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.papermc.paper.plugin.configuration.PluginMeta;
import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VersionCommandTest {

  @Mock private MobClashPlugin plugin;
  @Mock private SpawnManager spawnManager;
  @Mock private LanguageManager langManager;
  @Mock private CommandSender sender;
  @Mock private Command command;
  @Mock private PluginMeta pluginMeta;

  private VersionCommand versionCommand;

  @BeforeEach
  void setUp() {
    versionCommand = new VersionCommand(plugin, spawnManager, langManager);
    when(sender.hasPermission("mobclash.version")).thenReturn(true);
  }

  @Test
  void repliesWithThePluginAndServerVersion() {
    when(plugin.getPluginMeta()).thenReturn(pluginMeta);
    when(pluginMeta.getName()).thenReturn("MobClash");
    when(pluginMeta.getVersion()).thenReturn("2.2.0");
    when(langManager.getMessage("version-info", "MobClash", "2.2.0", "git-Paper-123 (MC: 1.21.11)"))
        .thenReturn("MobClash v2.2.0 on git-Paper-123 (MC: 1.21.11)");

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getVersion).thenReturn("git-Paper-123 (MC: 1.21.11)");

      assertTrue(versionCommand.onCommand(sender, command, "version", new String[0]));
    }

    verify(sender).sendMessage("MobClash v2.2.0 on git-Paper-123 (MC: 1.21.11)");
  }

  @Test
  void aSenderWithoutThePermissionIsRefused() {
    when(sender.hasPermission("mobclash.version")).thenReturn(false);
    when(langManager.getMessage("no-permission")).thenReturn("denied");

    assertTrue(versionCommand.onCommand(sender, command, "version", new String[0]));

    verify(sender).sendMessage("denied");
    verify(plugin, org.mockito.Mockito.never()).getPluginMeta();
  }
}
