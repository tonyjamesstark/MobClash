package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import org.bukkit.command.CommandSender;

/**
 * {@code /removegroup <group> [confirm]} and {@code /mobclash removegroup}: forget every spawn
 * point and chest of a group. Without {@code confirm} it only says what it would remove, since one
 * typo in the group name would otherwise lose a whole arena.
 */
public class RemoveGroupCommand extends BaseCommand {

  public RemoveGroupCommand(
      MobClashPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobclash.removegroup", false);
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    boolean confirmed = args.length == 2 && args[1].equalsIgnoreCase("confirm");
    if (args.length < 1 || args.length > 2 || (args.length == 2 && !confirmed)) {
      return false;
    }

    String groupName = args[0];
    int points = spawnManager.getSpawnPoints(groupName).size();
    int chests = spawnManager.getGroupWaves(groupName).size();

    // A group made with /setchest alone has chests but no spawn points.
    if (!spawnManager.hasGroup(groupName) && chests == 0) {
      reply(sender, "group-not-exist", groupName);
      return true;
    }

    if (!confirmed) {
      reply(sender, "removegroup-confirm", groupName, points, chests);
      return true;
    }

    spawnManager.removeGroup(groupName);
    reply(sender, "removegroup-success", groupName, points, chests);
    return true;
  }
}
