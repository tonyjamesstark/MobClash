package io.tjs.mobclash.commands;

import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SetChestCommand extends BaseCommand {

  public SetChestCommand(
      JavaPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobspawner.setchest", true);
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(langManager.getMessage("setchest-usage"));
      return true;
    }

    String groupName = args[0];
    String waveName = args[1];

    if (!spawnManager.hasGroup(groupName)) {
      sender.sendMessage(langManager.getMessage("group-not-exist", groupName));
      return true;
    }

    Player player = getPlayer(sender);
    Block targetBlock = player.getTargetBlock(null, 5);

    if (targetBlock.getType() != Material.CHEST) {
      sender.sendMessage(langManager.getMessage("must-look-chest"));
      return true;
    }

    spawnManager.setGroupChest(groupName, waveName, targetBlock.getLocation());
    sender.sendMessage(langManager.getMessage("setchest-success", groupName, waveName));
    return true;
  }
}
