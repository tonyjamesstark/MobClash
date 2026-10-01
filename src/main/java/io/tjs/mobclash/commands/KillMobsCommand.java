package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import org.bukkit.command.CommandSender;

/**
 * {@code /killmobs [group]} and {@code /mobclash killmobs}: remove every loaded MobClash mob, or
 * only one group's, without drops, XP or kill credit. Not {@code /killall}, which Essentials owns.
 */
public class KillMobsCommand extends BaseCommand {

  private final MobTracker mobTracker;

  public KillMobsCommand(
      MobClashPlugin plugin,
      SpawnManager spawnManager,
      LanguageManager langManager,
      MobTracker mobTracker) {
    super(plugin, spawnManager, langManager, "mobclash.killmobs", false);
    this.mobTracker = mobTracker;
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    if (args.length > 1) {
      return false;
    }

    if (args.length == 0) {
      reply(sender, "killmobs-success", mobTracker.removeMobs(null));
    } else {
      reply(sender, "killmobs-group-success", args[0], mobTracker.removeMobs(args[0]));
    }
    return true;
  }
}
