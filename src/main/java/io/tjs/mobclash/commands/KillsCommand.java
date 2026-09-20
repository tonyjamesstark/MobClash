package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KillsCommand extends BaseCommand {

  private final MobTracker mobTracker;

  public KillsCommand(
      MobClashPlugin plugin,
      SpawnManager spawnManager,
      LanguageManager langManager,
      MobTracker mobTracker) {
    super(plugin, spawnManager, langManager, "mobclash.kills", false);
    this.mobTracker = mobTracker;
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    // /kills - show your own kills
    if (args.length == 0) {
      if (!(sender instanceof Player)) {
        sender.sendMessage(langManager.getMessage("players-only"));
        return true;
      }

      Player player = (Player) sender;
      int kills = mobTracker.getKills(player);
      sender.sendMessage(langManager.getMessage("kills-personal", kills));
      return true;
    }

    String subcommand = args[0].toLowerCase();

    // /kills top [amount] - show leaderboard
    if (subcommand.equals("top")) {
      int limit = 10;
      if (args.length > 1) {
        try {
          limit = Integer.parseInt(args[1]);
          // Clamp both ends: Stream.limit throws on a negative value, and 0 would report an
          // empty leaderboard as "no kills recorded".
          limit = Math.max(1, Math.min(limit, 50));
        } catch (NumberFormatException e) {
          sender.sendMessage(langManager.getMessage("invalid-number"));
          return true;
        }
      }

      List<Map.Entry<UUID, Integer>> topKills = mobTracker.getTopKills(limit);

      if (topKills.isEmpty()) {
        sender.sendMessage(langManager.getMessage("kills-none"));
        return true;
      }

      sender.sendMessage(langManager.getMessage("kills-top-header", topKills.size()));
      int rank = 1;
      for (Map.Entry<UUID, Integer> entry : topKills) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
        String playerName = player.getName() != null ? player.getName() : "Unknown";
        sender.sendMessage(
            langManager.getMessage("kills-top-entry", rank, playerName, entry.getValue()));
        rank++;
      }
      return true;
    }

    // /kills reset - reset your own kills
    if (subcommand.equals("reset")) {
      if (!(sender instanceof Player)) {
        sender.sendMessage(langManager.getMessage("players-only"));
        return true;
      }

      Player player = (Player) sender;
      int oldKills = mobTracker.getKills(player);
      mobTracker.resetKills(player);
      mobTracker.saveKillData();
      sender.sendMessage(langManager.getMessage("kills-reset", oldKills));
      return true;
    }

    // /kills resetall - reset all kills (requires permission)
    if (subcommand.equals("resetall")) {
      if (!hasPermissionOrBypass(sender, "mobclash.kills.resetall")) {
        sender.sendMessage(langManager.getMessage("no-permission"));
        return true;
      }

      mobTracker.resetAllKills();
      mobTracker.saveKillData();
      sender.sendMessage(langManager.getMessage("kills-resetall"));
      return true;
    }

    // Unknown subcommand
    sender.sendMessage(langManager.getMessage("kills-usage"));
    return true;
  }
}
