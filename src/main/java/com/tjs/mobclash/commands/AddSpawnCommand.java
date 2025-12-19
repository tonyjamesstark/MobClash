package com.example.mobspawner.commands;

import com.example.mobspawner.managers.LanguageManager;
import com.example.mobspawner.managers.SpawnManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AddSpawnCommand extends BaseCommand {

    public AddSpawnCommand(JavaPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
        super(plugin, spawnManager, langManager, "mobspawner.addspawn", true);
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(langManager.getMessage("addspawn-usage"));
            return true;
        }
        
        Player player = getPlayer(sender);
        String groupName = args[0];
        
        spawnManager.addSpawnPoint(groupName, player.getLocation());
        
        sender.sendMessage(langManager.getMessage("addspawn-success", 
            groupName, spawnManager.getSpawnPoints(groupName).size()));
        return true;
    }
}