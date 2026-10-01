package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public abstract class BaseCommand implements CommandExecutor {

  protected final MobClashPlugin plugin;
  protected final SpawnManager spawnManager;
  protected final LanguageManager langManager;
  protected final String permission;
  protected final boolean requiresPlayer;
  private String usage = "";

  private static final String COMMAND = "/<command>";

  // Listed rather than read from Tag.SHULKER_BOXES, which resolves through the running server and
  // so cannot load in a unit test.
  private static final Set<Material> CHEST_BLOCKS =
      EnumSet.of(
          Material.CHEST,
          Material.BARREL,
          Material.SHULKER_BOX,
          Material.WHITE_SHULKER_BOX,
          Material.ORANGE_SHULKER_BOX,
          Material.MAGENTA_SHULKER_BOX,
          Material.LIGHT_BLUE_SHULKER_BOX,
          Material.YELLOW_SHULKER_BOX,
          Material.LIME_SHULKER_BOX,
          Material.PINK_SHULKER_BOX,
          Material.GRAY_SHULKER_BOX,
          Material.LIGHT_GRAY_SHULKER_BOX,
          Material.CYAN_SHULKER_BOX,
          Material.PURPLE_SHULKER_BOX,
          Material.BLUE_SHULKER_BOX,
          Material.BROWN_SHULKER_BOX,
          Material.GREEN_SHULKER_BOX,
          Material.RED_SHULKER_BOX,
          Material.BLACK_SHULKER_BOX);

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

    try {
      if (!execute(sender, args)) {
        sendUsage(sender, label);
      }
      return true;
    } catch (RuntimeException e) {
      plugin
          .getLogger()
          .log(
              Level.SEVERE,
              "Command '" + command.getName() + "' threw for args " + Arrays.toString(args),
              e);
      reply(sender, "command-error");
      return true;
    }
  }

  /** Whether the sender may run this command, with the console and command block bypass. */
  public boolean canUse(CommandSender sender) {
    return hasPermissionOrBypass(sender, permission);
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
   * The inventory of a block that can be a wave or gear chest, or null for any other block. A plain
   * chest, a barrel and a shulker box of any colour count, since they only hold items. Hoppers,
   * droppers, dispensers, furnaces and crafters are containers too, but they move or use what is in
   * them. Trapped and copper chests are refused as well, and Bukkit gives both a Chest state, so
   * the rule reads the block's material rather than its state. A double chest yields both halves.
   */
  protected static Inventory chestInventory(Block block) {
    return CHEST_BLOCKS.contains(block.getType())
        ? ((Container) block.getState()).getInventory()
        : null;
  }

  /**
   * Send the sender a language.yml message and, when the sender is a command block, also log it
   * through the console naming the block's world and position. A command block's LastOutput is
   * otherwise the only place the reason for an early exit or a skipped egg ever appears, invisible
   * to the operator debugging a production chest from the server log.
   */
  protected void reply(CommandSender sender, String key, Object... args) {
    send(sender, langManager.getMessage(key, args));
  }

  /** Send a message, also logging it when the sender is a command block. See {@link #reply}. */
  private void send(CommandSender sender, String message) {
    sender.sendMessage(message);
    if (sender instanceof BlockCommandSender) {
      Location loc = senderLocation(sender);
      plugin.log(
          Level.INFO,
          "Command block at "
              + loc.getWorld().getName()
              + " "
              + loc.getBlockX()
              + " "
              + loc.getBlockY()
              + " "
              + loc.getBlockZ()
              + ": "
              + ChatColor.stripColor(message));
    }
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

  /**
   * Set this command's usage, the block from its plugin.yml entry. plugin.yml is the one place
   * usage and help text live: Bukkit's /help reads it too, and unlike language.yml the server's
   * copy is replaced on every upgrade, so a new argument cannot leave a stale usage line behind.
   */
  public void setUsage(String usage) {
    this.usage = usage == null ? "" : usage;
  }

  /**
   * The usage lines that show a form of the command, for {@code /mobclash help}. {@code typed} is
   * what stands in for {@code /<command>}, without the slash.
   */
  public List<String> helpLines(String typed) {
    return usage
        .lines()
        .filter(line -> line.startsWith(COMMAND))
        .map(line -> render(line, typed))
        .toList();
  }

  private void sendUsage(CommandSender sender, String typed) {
    usage.lines().map(line -> render(line, typed)).forEach(line -> send(sender, line));
  }

  /**
   * One usage line in chat colours: the command form yellow and its explanation after " - " grey. A
   * line that is not a command form is all grey.
   */
  static String render(String line, String typed) {
    String text = line.replace(COMMAND, "/" + typed);
    if (!line.startsWith(COMMAND)) {
      return "§7" + text;
    }
    int dash = text.indexOf(" - ");
    return dash < 0
        ? "§e" + text
        : "§e" + text.substring(0, dash) + " §7" + text.substring(dash + 1);
  }

  /**
   * Execute the command logic. Return false for arguments that do not fit, and the usage from
   * plugin.yml is shown; return true once the sender has been told what happened.
   */
  protected abstract boolean execute(CommandSender sender, String[] args);
}
