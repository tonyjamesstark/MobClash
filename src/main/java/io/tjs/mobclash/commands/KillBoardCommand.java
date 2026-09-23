package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.KillBoard;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KillBoardCommand extends BaseCommand {

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
    // /killboard - toggle your own
    if (args.length == 0) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(langManager.getMessage("players-only"));
        return true;
      }
      boolean showing = killBoard.toggle(player);
      sender.sendMessage(langManager.getMessage(showing ? "killboard-on" : "killboard-off"));
      return true;
    }

    String subcommand = args[0].toLowerCase();
    if (!subcommand.equals("world") && !subcommand.equals("alloff")) {
      sender.sendMessage(langManager.getMessage("killboard-usage"));
      return true;
    }

    if (!hasPermissionOrBypass(sender, "mobclash.killboard.admin")) {
      sender.sendMessage(langManager.getMessage("no-permission"));
      return true;
    }

    // /killboard alloff - everyone, every world
    if (subcommand.equals("alloff")) {
      sender.sendMessage(langManager.getMessage("killboard-alloff", killBoard.hideAll()));
      return true;
    }

    // /killboard world <on|off> - everyone in the sender's world
    String state = args.length > 1 ? args[1].toLowerCase() : "";
    if (!state.equals("on") && !state.equals("off")) {
      sender.sendMessage(langManager.getMessage("killboard-usage"));
      return true;
    }
    Location here = senderLocation(sender);
    if (here == null) {
      sender.sendMessage(langManager.getMessage("no-location"));
      return true;
    }

    boolean on = state.equals("on");
    int changed = 0;
    for (Player player : here.getWorld().getPlayers()) {
      if (on ? killBoard.show(player) : killBoard.hide(player)) {
        changed++;
      }
    }
    sender.sendMessage(
        langManager.getMessage(
            on ? "killboard-world-on" : "killboard-world-off", changed, here.getWorld().getName()));
    return true;
  }
}
