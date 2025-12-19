package com.example.mobclash.commands;

import com.example.mobclash.MobClashPlugin;
import com.example.mobclash.managers.LanguageManager;
import com.example.mobclash.managers.SpawnManager;
import java.util.logging.Level;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class BaseCommand implements CommandExecutor {

  protected final JavaPlugin plugin;
  protected final SpawnManager spawnManager;
  protected final LanguageManager langManager;
  protected final String permission;
  protected final boolean requiresPlayer;

  public BaseCommand(
      JavaPlugin plugin,
      SpawnManager spawnManager,
      LanguageManager langManager,
      String permission,
      boolean requiresPlayer) {
    this.plugin = plugin;
    this.spawnManager = spawnManager;
    this.langManager = langManager;
    this.permission = permission;
    this.requiresPlayer = requiresPlayer;
  }

  @Override
  public final boolean onCommand(
      CommandSender sender, Command command, String label, String[] args) {
    ((MobClashPlugin) plugin)
        .log(
            Level.INFO,
            "Command '"
                + command.getName()
                + "' executed by "
                + sender.getName()
                + " (type: "
                + sender.getClass().getSimpleName()
                + ")");

    // Allow command blocks to bypass permission checks
    if (!isCommandBlock(sender)) {
      if (!sender.hasPermission(permission)) {
        ((MobClashPlugin) plugin)
            .log(
                Level.INFO,
                "Permission denied for " + sender.getName() + " - requires: " + permission);
        sender.sendMessage(langManager.getMessage("no-permission"));
        return true;
      }
    } else {
      ((MobClashPlugin) plugin).log(Level.INFO, "Command block bypassing permission check");
    }

    // Check if command requires a player
    if (requiresPlayer && !(sender instanceof Player)) {
      ((MobClashPlugin) plugin)
          .log(
              Level.INFO,
              "Command requires player but sender is " + sender.getClass().getSimpleName());
      sender.sendMessage(langManager.getMessage("players-only"));
      return true;
    }

    return execute(sender, args);
  }

  protected boolean isCommandBlock(CommandSender sender) {
    return sender instanceof BlockCommandSender;
  }

  protected Player getPlayer(CommandSender sender) {
    return (Player) sender;
  }

  /** Execute the command logic */
  protected abstract boolean execute(CommandSender sender, String[] args);
}
