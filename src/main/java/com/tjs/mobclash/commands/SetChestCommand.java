package com.example.mobclash.commands;

import com.example.mobclash.managers.LanguageManager;
import com.example.mobclash.managers.SpawnManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SetChestCommand extends BaseCommand {

    public SetChestCommand(JavaPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
        super(plugin, spawnManager, langManager, "mobspawner.setchest", true);
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(langManager.getMessage("setchest-usage"));
            return true;
        }
        
        String groupName = args[0];
        
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
        
        spawnManager.setGroupChest(groupName, targetBlock.getLocation());
        sender.sendMessage(langManager.getMessage("setchest-success", groupName));
        return true;
    }
}