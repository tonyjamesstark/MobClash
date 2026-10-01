package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.KillBoard;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * {@code /killboard}: the kill leaderboard sidebar.
 *
 * <ul>
 *   <li>{@code /killboard} toggles it for the sender.
 *   <li>{@code /killboard <on|off>} sets it for the sender explicitly. Players only.
 *   <li>{@code /killboard world <on|off>} switches it for everyone in the sender's world.
 *   <li>{@code /killboard world <on|off> <world>} and the shorthand {@code /killboard <world>
 *       <on|off>} switch it for a named world instead. The console may use these.
 *   <li>{@code /killboard alloff} turns it off everywhere.
 * </ul>
 *
 * The world forms need {@code mobclash.killboard.admin}. A world name matches case-insensitively
 * against the currently loaded worlds, or as a namespaced key such as {@code
 * minecraft:monstermash}.
 */
public class KillBoardCommand extends BaseCommand implements TabCompleter {

  private static final String ADMIN_PERMISSION = "mobclash.killboard.admin";
  private static final List<String> ON_OFF = List.of("on", "off");

  private final KillBoard killBoard;

  public KillBoardCommand(
      MobClashPlugin plugin,
      SpawnManager spawnManager,
      LanguageManager langManager,
      KillBoard killBoard) {
    super(plugin, spawnManager, langManager, "mobclash.killboard", false);
    this.killBoard = killBoard;
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      return toggleOwn(sender);
    }

    String first = args[0].toLowerCase(Locale.ROOT);
    if (args.length == 1 && ON_OFF.contains(first)) {
      return setOwn(sender, first.equals("on"));
    }
    if (args.length == 1 && first.equals("alloff")) {
      return allOff(sender);
    }
    if (first.equals("world")) {
      return world(sender, args);
    }
    if (args.length == 2 && ON_OFF.contains(args[1].toLowerCase(Locale.ROOT))) {
      // /killboard <world> <on|off>
      return worldByName(sender, args[0], args[1].equalsIgnoreCase("on"));
    }

    return false;
  }

  private boolean toggleOwn(CommandSender sender) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(langManager.getMessage("players-only"));
      return true;
    }
    boolean showing = killBoard.toggle(player);
    sender.sendMessage(langManager.getMessage(showing ? "killboard-on" : "killboard-off"));
    return true;
  }

  private boolean setOwn(CommandSender sender, boolean on) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(langManager.getMessage("players-only"));
      return true;
    }
    if (on) {
      killBoard.show(player);
    } else {
      killBoard.hide(player);
    }
    sender.sendMessage(langManager.getMessage(on ? "killboard-on" : "killboard-off"));
    return true;
  }

  private boolean allOff(CommandSender sender) {
    if (!requireAdmin(sender)) {
      return true;
    }
    sender.sendMessage(langManager.getMessage("killboard-alloff", killBoard.hideAll()));
    return true;
  }

  // /killboard world <on|off> [world]
  private boolean world(CommandSender sender, String[] args) {
    if (!requireAdmin(sender)) {
      return true;
    }
    if (args.length < 2) {
      return false;
    }
    String state = args[1].toLowerCase(Locale.ROOT);
    if (!ON_OFF.contains(state)) {
      return false;
    }
    boolean on = state.equals("on");
    if (args.length == 2) {
      Location here = senderLocation(sender);
      if (here == null) {
        sender.sendMessage(langManager.getMessage("no-location"));
        return true;
      }
      applyToWorld(sender, here.getWorld(), on);
      return true;
    }
    if (args.length == 3) {
      return resolveAndApply(sender, args[2], on);
    }
    return false;
  }

  // /killboard <world> <on|off>
  private boolean worldByName(CommandSender sender, String worldName, boolean on) {
    if (!requireAdmin(sender)) {
      return true;
    }
    return resolveAndApply(sender, worldName, on);
  }

  private boolean resolveAndApply(CommandSender sender, String worldName, boolean on) {
    World world = resolveWorld(plugin.getServer(), worldName);
    if (world == null) {
      sender.sendMessage(langManager.getMessage("killboard-unknown-world", worldName));
      return true;
    }
    applyToWorld(sender, world, on);
    return true;
  }

  private void applyToWorld(CommandSender sender, World world, boolean on) {
    int changed = on ? killBoard.enableWorld(world) : killBoard.disableWorld(world);
    sender.sendMessage(
        langManager.getMessage(
            on ? "killboard-world-on" : "killboard-world-off", changed, world.getName()));
  }

  private boolean requireAdmin(CommandSender sender) {
    if (hasPermissionOrBypass(sender, ADMIN_PERMISSION)) {
      return true;
    }
    sender.sendMessage(langManager.getMessage("no-permission"));
    return false;
  }

  /**
   * A loaded world matching {@code name}, case-insensitively against the server's own world names,
   * or as a namespaced key such as {@code minecraft:monstermash}. Bukkit's own {@code
   * getWorld(String)} is case-insensitive on CraftBukkit too, but that is undocumented behaviour;
   * this compares names directly instead.
   */
  static World resolveWorld(Server server, String name) {
    for (World world : server.getWorlds()) {
      if (world.getName().equalsIgnoreCase(name)) {
        return world;
      }
    }
    NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
    return key == null ? null : server.getWorld(key);
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    boolean canSelf = hasPermissionOrBypass(sender, permission);
    boolean admin = hasPermissionOrBypass(sender, ADMIN_PERMISSION);
    String typed = args[args.length - 1].toLowerCase(Locale.ROOT);

    if (args.length == 1) {
      Stream<String> options = canSelf ? ON_OFF.stream() : Stream.empty();
      if (admin) {
        options = Stream.concat(options, Stream.concat(Stream.of("world", "alloff"), worldNames()));
      }
      return options.filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    if (args.length == 2 && admin) {
      String first = args[0].toLowerCase(Locale.ROOT);
      if (first.equals("world") || !ON_OFF.contains(first) && !first.equals("alloff")) {
        return ON_OFF.stream().filter(option -> option.startsWith(typed)).toList();
      }
      return List.of();
    }

    if (args.length == 3
        && admin
        && args[0].equalsIgnoreCase("world")
        && ON_OFF.contains(args[1].toLowerCase(Locale.ROOT))) {
      return worldNames().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    return List.of();
  }

  private Stream<String> worldNames() {
    return plugin.getServer().getWorlds().stream().map(World::getName);
  }
}
