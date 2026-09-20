package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;

public class ShowSpawnsCommand extends BaseCommand {

  public ShowSpawnsCommand(
      MobClashPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobclash.showspawns", false);
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

    // Particles rather than AreaEffectCloud entities: a marker should not appear in @e
    // selectors, fire EntitySpawnEvent for every other plugin, or be written into the chunk.
    // The old setColor(Color.RED) was inert anyway, since FLAME is not a colourable particle.
    for (Location loc : locations) {
      loc.getWorld()
          .spawnParticle(Particle.FLAME, loc.clone().add(0.5, 0.5, 0.5), 40, 0.3, 0.6, 0.3, 0.01);
    }

    sender.sendMessage(langManager.getMessage("showspawns-success", groupName, locations.size()));
    return true;
  }
}
