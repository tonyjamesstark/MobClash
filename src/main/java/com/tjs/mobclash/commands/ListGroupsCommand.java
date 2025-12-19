package com.example.mobclash.commands;

import com.example.mobclash.managers.LanguageManager;
import com.example.mobclash.managers.SpawnManager;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class ListGroupsCommand extends BaseCommand {

  public ListGroupsCommand(
      JavaPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobspawner.listgroups", false);
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    Map<String, List<Location>> groups = spawnManager.getAllGroups();

    if (groups.isEmpty()) {
      sender.sendMessage(langManager.getMessage("no-groups"));
      return true;
    }

    sender.sendMessage(langManager.getMessage("listgroups-header"));
    for (Map.Entry<String, List<Location>> entry : groups.entrySet()) {
      sender.sendMessage(
          langManager.getMessage("listgroups-entry", entry.getKey(), entry.getValue().size()));
    }

    return true;
  }
}
