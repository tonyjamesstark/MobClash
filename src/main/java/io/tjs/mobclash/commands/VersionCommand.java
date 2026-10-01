package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/** {@code /mobclash version}. Not a standalone command: {@code /version} is the server's. */
public class VersionCommand extends BaseCommand {

  public VersionCommand(
      MobClashPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobclash.version", false);
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    sender.sendMessage(
        langManager.getMessage(
            "version-info",
            plugin.getPluginMeta().getName(),
            plugin.getPluginMeta().getVersion(),
            Bukkit.getVersion()));
    return true;
  }
}
