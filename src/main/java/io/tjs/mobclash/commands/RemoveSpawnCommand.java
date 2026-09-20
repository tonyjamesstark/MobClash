package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

public class RemoveSpawnCommand extends BaseCommand {

  public RemoveSpawnCommand(
      MobClashPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobclash.removespawn", false);
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    if (args.length < 1) {
      sender.sendMessage(langManager.getMessage("removespawn-usage"));
      return true;
    }

    String groupName = args[0];

    if (!spawnManager.hasGroup(groupName)) {
      sender.sendMessage(langManager.getMessage("group-not-exist", groupName));
      return true;
    }

    if (spawnManager.getSpawnPoints(groupName).isEmpty()) {
      sender.sendMessage(langManager.getMessage("group-no-points", groupName));
      return true;
    }

    // "Nearest" is measured from the command block's own position when one runs this.
    Location location = senderLocation(sender);
    if (location == null) {
      sender.sendMessage(langManager.getMessage("no-location"));
      return true;
    }

    if (spawnManager.removeNearestSpawnPoint(groupName, location)) {
      sender.sendMessage(langManager.getMessage("removespawn-success", groupName));
    } else {
      sender.sendMessage(langManager.getMessage("group-no-points", groupName));
    }

    return true;
  }
}
