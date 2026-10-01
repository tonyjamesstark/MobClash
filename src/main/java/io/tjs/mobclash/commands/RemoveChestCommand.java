package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import org.bukkit.command.CommandSender;

/**
 * {@code /removechest <group> <chest>} and {@code /mobclash removechest}: forget a chest set with
 * {@code /setchest}, such as one that was moved or broken. The block itself is left alone.
 */
public class RemoveChestCommand extends BaseCommand {

  public RemoveChestCommand(
      MobClashPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobclash.removechest", false);
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    if (args.length != 2) {
      return false;
    }

    String groupName = args[0];
    String chestName = args[1];

    if (spawnManager.removeGroupChest(groupName, chestName)) {
      reply(sender, "removechest-success", groupName, chestName);
    } else {
      reply(sender, "chest-not-set", groupName, chestName);
    }
    return true;
  }
}
