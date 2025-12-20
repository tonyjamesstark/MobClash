package io.tjs.mobclash.commands;

import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ListSpawnsCommand extends BaseCommand {

  public ListSpawnsCommand(
      JavaPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobspawner.listspawns", false);
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    if (args.length < 1) {
      sender.sendMessage(langManager.getMessage("listspawns-usage"));
      return true;
    }

    String groupName = args[0];

    if (!spawnManager.hasGroup(groupName)) {
      sender.sendMessage(langManager.getMessage("group-not-exist", groupName));
      return true;
    }

    List<Location> locations = spawnManager.getSpawnPoints(groupName);

    if (locations.isEmpty()) {
      sender.sendMessage(langManager.getMessage("group-no-points", groupName));
      return true;
    }

    sender.sendMessage(langManager.getMessage("listspawns-header", groupName, locations.size()));

    for (int i = 0; i < locations.size(); i++) {
      Location loc = locations.get(i);
      sender.sendMessage(
          langManager.getMessage(
              "listspawns-entry",
              i + 1,
              loc.getWorld().getName(),
              Math.round(loc.getX()),
              Math.round(loc.getY()),
              Math.round(loc.getZ())));
    }

    return true;
  }
}
