package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

/**
 * {@code /removespawn <group> [number]}: without a number, remove the group's point nearest the
 * sender; with one, the point /listspawns shows under that number, from wherever the sender is.
 */
public class RemoveSpawnCommand extends BaseCommand {

  public RemoveSpawnCommand(
      MobClashPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobclash.removespawn", false);
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    if (args.length < 1 || args.length > 2) {
      return false;
    }

    String groupName = args[0];

    if (!spawnManager.hasGroup(groupName)) {
      reply(sender, "group-not-exist", groupName);
      return true;
    }

    int count = spawnManager.getSpawnPoints(groupName).size();
    if (count == 0) {
      reply(sender, "group-no-points", groupName);
      return true;
    }

    if (args.length == 2) {
      removeNumbered(sender, groupName, args[1], count);
    } else {
      removeNearest(sender, groupName);
    }
    return true;
  }

  private void removeNumbered(CommandSender sender, String groupName, String typed, int count) {
    int number = typed.matches("\\d{1,9}") ? Integer.parseInt(typed) : 0;
    if (number < 1 || number > count) {
      reply(sender, "invalid-spawn-number", groupName, typed, count);
      return;
    }

    Location removed = spawnManager.removeSpawnPoint(groupName, number - 1);
    String worldName = SpawnManager.worldNameOf(removed);
    reply(
        sender,
        "removespawn-number-success",
        groupName,
        number,
        worldName == null ? langManager.getMessage("world-unloaded") : worldName,
        Math.round(removed.getX()),
        Math.round(removed.getY()),
        Math.round(removed.getZ()));
  }

  private void removeNearest(CommandSender sender, String groupName) {
    // "Nearest" is measured from the command block's own position when one runs this.
    Location location = senderLocation(sender);
    if (location == null) {
      reply(sender, "no-location");
      return;
    }

    if (spawnManager.removeNearestSpawnPoint(groupName, location)) {
      reply(sender, "removespawn-success", groupName);
    } else {
      reply(sender, "group-no-points", groupName);
    }
  }
}
