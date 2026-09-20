package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

public class AddSpawnCommand extends BaseCommand {

  public AddSpawnCommand(
      MobClashPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobclash.addspawn", false);
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    if (args.length < 1) {
      sender.sendMessage(langManager.getMessage("addspawn-usage"));
      return true;
    }

    String groupName = args[0];
    if (!validateName(sender, groupName)) {
      return true;
    }

    // A command block adds a point at its own position; a player at theirs.
    Location location = senderLocation(sender);
    if (location == null) {
      sender.sendMessage(langManager.getMessage("no-location"));
      return true;
    }

    spawnManager.addSpawnPoint(groupName, location);

    sender.sendMessage(
        langManager.getMessage(
            "addspawn-success", groupName, spawnManager.getSpawnPoints(groupName).size()));
    return true;
  }
}
