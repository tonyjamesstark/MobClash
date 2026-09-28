package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;

/** {@code /mobclash reload}. Not a standalone command: {@code /reload} is the server's. */
public class ReloadCommand extends BaseCommand {

  public ReloadCommand(
      MobClashPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobclash.reload", false);
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    try {
      plugin.reloadSettings();
    } catch (InvalidConfigurationException e) {
      // Always logged: the parser's line and column go to the console, where the file is edited.
      plugin
          .getLogger()
          .log(Level.WARNING, "Not reloaded: " + e.getMessage() + " does not parse", e);
      sender.sendMessage(langManager.getMessage("reload-failed", e.getMessage()));
      return true;
    }
    sender.sendMessage(langManager.getMessage("reload-success"));
    return true;
  }
}
