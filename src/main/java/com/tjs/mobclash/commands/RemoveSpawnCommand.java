package com.example.mobclash.commands;

import com.example.mobclash.managers.LanguageManager;
import com.example.mobclash.managers.SpawnManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RemoveSpawnCommand extends BaseCommand {

    public RemoveSpawnCommand(JavaPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
        super(plugin, spawnManager, langManager, "mobspawner.removespawn", true);
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(langManager.getMessage("removespawn-usage"));
            return true;
        }
        
        Player player = getPlayer(sender);
        String groupName = args[0];
        
        if (!spawnManager.hasGroup(groupName)) {
            sender.sendMessage(langManager.getMessage("group-not-exist", groupName));
            return true;
        }
        
        if (spawnManager.getSpawnPoints(groupName).isEmpty()) {
            sender.sendMessage(langManager.getMessage("group-no-points", groupName));
            return true;
        }
        
        if (spawnManager.removeNearestSpawnPoint(groupName, player.getLocation())) {
            sender.sendMessage(langManager.getMessage("removespawn-success", groupName));
        }
        
        return true;
    }
}