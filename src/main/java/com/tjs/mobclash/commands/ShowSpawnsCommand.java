package com.example.mobclash.commands;

import com.example.mobclash.managers.LanguageManager;
import com.example.mobclash.managers.SpawnManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ShowSpawnsCommand extends BaseCommand {

    public ShowSpawnsCommand(JavaPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
        super(plugin, spawnManager, langManager, "mobspawner.showspawns", false);
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(langManager.getMessage("showspawns-usage"));
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
        
        for (Location loc : locations) {
            AreaEffectCloud cloud = (AreaEffectCloud) loc.getWorld().spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);
            cloud.setDuration(60); // 3 seconds
            cloud.setRadius(1.0f);
            cloud.setParticle(Particle.FLAME);
            cloud.setColor(Color.RED);
        }
        
        sender.sendMessage(langManager.getMessage("showspawns-success", groupName, locations.size()));
        return true;
    }
}