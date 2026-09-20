package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

public class ListGroupsCommand extends BaseCommand {

  public ListGroupsCommand(
      MobClashPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobclash.listgroups", false);
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
