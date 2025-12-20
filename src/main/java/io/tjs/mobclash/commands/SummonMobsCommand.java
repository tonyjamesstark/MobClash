package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SummonMobsCommand extends BaseCommand {

  public SummonMobsCommand(
      MobClashPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobspawner.summon", false);
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    if (args.length < 3) {
      sender.sendMessage(langManager.getMessage("summonmobs-usage"));
      return true;
    }

    String groupName = args[0];
    String waveName = args[1];
    String mode = args[2].toLowerCase();

    if (!mode.equals("random") && !mode.equals("all")) {
      sender.sendMessage(langManager.getMessage("invalid-mode"));
      return true;
    }

    if (!spawnManager.hasGroup(groupName)) {
      sender.sendMessage(langManager.getMessage("group-not-exist", groupName));
      return true;
    }

    Location chestLocation = spawnManager.getGroupChest(groupName, waveName);
    if (chestLocation == null) {
      sender.sendMessage(langManager.getMessage("wave-not-set", groupName, waveName));
      return true;
    }

    List<Location> locations = spawnManager.getSpawnPoints(groupName);
    if (locations.isEmpty()) {
      sender.sendMessage(langManager.getMessage("group-no-points", groupName));
      return true;
    }

    int amount = parseAmount(sender, args);
    if (amount == -1) {
      return true;
    }

    Block block = chestLocation.getBlock();
    if (block.getType() != Material.CHEST) {
      sender.sendMessage(langManager.getMessage("chest-missing"));
      return true;
    }

    Chest chest = (Chest) block.getState();
    List<EntityType> spawnEggs = getSpawnEggsFromChest(chest.getInventory());

    if (spawnEggs.isEmpty()) {
      sender.sendMessage(langManager.getMessage("no-spawn-eggs"));
      return true;
    }

    ((MobClashPlugin) plugin)
        .log(
            Level.INFO,
            "Summoning "
                + amount
                + " mob(s) for group '"
                + groupName
                + "' wave '"
                + waveName
                + "' in mode '"
                + mode
                + "' by "
                + sender.getName());
    ((MobClashPlugin) plugin)
        .log(Level.INFO, "Available mob types: " + getUniqueMobTypes(spawnEggs));

    int spawned = summonMobs(mode, locations, spawnEggs, amount, groupName, waveName);

    ((MobClashPlugin) plugin)
        .log(
            Level.INFO,
            "Successfully spawned "
                + spawned
                + " mob(s) for group '"
                + groupName
                + "' wave '"
                + waveName
                + "'");

    sender.sendMessage(langManager.getMessage("summonmobs-success", spawned, groupName, waveName));
    return true;
  }

  private int parseAmount(CommandSender sender, String[] args) {
    if (args.length <= 3) {
      return 1;
    }

    try {
      int amount = Integer.parseInt(args[3]);
      if (amount < 1 || amount > 100) {
        sender.sendMessage(langManager.getMessage("invalid-amount"));
        return -1;
      }
      return amount;
    } catch (NumberFormatException e) {
      sender.sendMessage(langManager.getMessage("invalid-number"));
      return -1;
    }
  }

  private List<EntityType> getSpawnEggsFromChest(Inventory inv) {
    List<EntityType> spawnEggs = new ArrayList<>();

    for (ItemStack item : inv.getContents()) {
      if (item != null && item.getType().toString().endsWith("_SPAWN_EGG")) {
        String mobName = item.getType().toString().replace("_SPAWN_EGG", "");
        try {
          EntityType entityType = EntityType.valueOf(mobName);
          for (int i = 0; i < item.getAmount(); i++) {
            spawnEggs.add(entityType);
          }
        } catch (IllegalArgumentException e) {
          // Invalid entity type, skip
        }
      }
    }

    return spawnEggs;
  }

  private String getUniqueMobTypes(List<EntityType> spawnEggs) {
    return spawnEggs.stream()
        .distinct()
        .map(EntityType::name)
        .reduce((a, b) -> a + ", " + b)
        .orElse("none");
  }

  private int summonMobs(
      String mode,
      List<Location> locations,
      List<EntityType> spawnEggs,
      int amount,
      String groupName,
      String waveName) {
    int spawned = 0;
    MobTracker mobTracker = ((MobClashPlugin) plugin).getMobTracker();

    if (mode.equals("random")) {
      Location spawnLoc = locations.get(spawnManager.getRandom().nextInt(locations.size()));
      ((MobClashPlugin) plugin)
          .log(
              Level.INFO,
              "Spawning at random location: "
                  + String.format(
                      "(%.1f, %.1f, %.1f)", spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));

      for (int i = 0; i < amount; i++) {
        EntityType entityType = spawnEggs.get(spawnManager.getRandom().nextInt(spawnEggs.size()));
        org.bukkit.entity.Entity entity = spawnLoc.getWorld().spawnEntity(spawnLoc, entityType);

        // Tag the mob
        if (entity instanceof org.bukkit.entity.LivingEntity) {
          mobTracker.tagMob((org.bukkit.entity.LivingEntity) entity, groupName, waveName);
        }
        spawned++;
      }
    } else {
      ((MobClashPlugin) plugin)
          .log(Level.INFO, "Spawning at all " + locations.size() + " locations");

      for (Location spawnLoc : locations) {
        for (int i = 0; i < amount; i++) {
          EntityType entityType = spawnEggs.get(spawnManager.getRandom().nextInt(spawnEggs.size()));
          org.bukkit.entity.Entity entity = spawnLoc.getWorld().spawnEntity(spawnLoc, entityType);

          // Tag the mob
          if (entity instanceof org.bukkit.entity.LivingEntity) {
            mobTracker.tagMob((org.bukkit.entity.LivingEntity) entity, groupName, waveName);
          }
          spawned++;
        }
      }
    }

    return spawned;
  }
}
