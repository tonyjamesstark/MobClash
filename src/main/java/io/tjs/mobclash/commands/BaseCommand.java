package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public abstract class BaseCommand implements CommandExecutor {

  protected final MobClashPlugin plugin;
  protected final SpawnManager spawnManager;
  protected final LanguageManager langManager;
  protected final String permission;
  protected final boolean requiresPlayer;

  public BaseCommand(
      MobClashPlugin plugin,
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
    plugin.log(
        Level.INFO,
        "Command '"
            + command.getName()
            + "' executed by "
            + sender.getName()
            + " (type: "
            + sender.getClass().getSimpleName()
            + ")");

    // Command blocks and console bypass permission checks
    if (!bypassesPermissions(sender)) {
      if (!sender.hasPermission(permission)) {
        plugin.log(
            Level.INFO, "Permission denied for " + sender.getName() + " - requires: " + permission);
        sender.sendMessage(langManager.getMessage("no-permission"));
        return true;
      }
    } else {
      plugin.log(Level.INFO, "Sender bypasses permission check");
    }

    // Check if command requires a player
    if (requiresPlayer && !(sender instanceof Player)) {
      plugin.log(
          Level.INFO, "Command requires player but sender is " + sender.getClass().getSimpleName());
      sender.sendMessage(langManager.getMessage("players-only"));
      return true;
    }

    return execute(sender, args);
  }

  /** The node that gates this command, for registration to hand to Bukkit. */
  public String getPermission() {
    return permission;
  }

  /**
   * Senders exempt from permission checks. Console is listed explicitly rather than relying on it
   * being an operator, which is the undocumented mechanism that made it work before.
   */
  protected boolean bypassesPermissions(CommandSender sender) {
    return sender instanceof BlockCommandSender || sender instanceof ConsoleCommandSender;
  }

  /**
   * Check a permission, honouring the same bypass as the outer gate. Subcommands with their own
   * permission node must route through this rather than calling hasPermission directly.
   */
  protected boolean hasPermissionOrBypass(CommandSender sender, String node) {
    return bypassesPermissions(sender) || sender.hasPermission(node);
  }

  /**
   * Where the sender is: a player's position, or the command block's own block. Null for any other
   * sender, which has no position to speak of.
   */
  protected Location senderLocation(CommandSender sender) {
    if (sender instanceof Player player) {
      return player.getLocation();
    }
    if (sender instanceof BlockCommandSender block) {
      return block.getBlock().getLocation();
    }
    return null;
  }

  /**
   * Reject group and wave names that cannot round-trip through the config, messaging the sender.
   * Returns true when the name is usable.
   */
  protected boolean validateName(CommandSender sender, String name) {
    if (SpawnManager.isValidName(name)) {
      return true;
    }
    sender.sendMessage(langManager.getMessage("invalid-name", name));
    return false;
  }

  protected Player getPlayer(CommandSender sender) {
    return (Player) sender;
  }

  /** Execute the command logic */
  protected abstract boolean execute(CommandSender sender, String[] args);
}
