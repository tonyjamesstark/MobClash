package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SummonMobsCommand extends BaseCommand {

  public SummonMobsCommand(
      MobClashPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobclash.summon", false);
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

    // "all" mode spawns amount * locations.size() entities in one synchronous tick, and nothing
    // bounds the number of spawn points. Cap the product, which is what actually lands.
    int plannedTotal = mode.equals("all") ? amount * locations.size() : amount;
    int maxPerSummon = plugin.getConfig().getInt("max-mobs-per-summon", 500);
    if (maxPerSummon > 0 && plannedTotal > maxPerSummon) {
      sender.sendMessage(langManager.getMessage("summon-too-large", plannedTotal, maxPerSummon));
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

    plugin.log(
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
    plugin.log(Level.INFO, "Available mob types: " + getUniqueMobTypes(spawnEggs));

    int spawned = summonMobs(mode, locations, spawnEggs, amount, groupName, waveName);

    plugin.log(
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

  /**
   * The entity an egg spawns, resolved through the namespaced key both sides share.
   *
   * <p>Matching the enum names instead (MOOSHROOM_SPAWN_EGG -> EntityType.MOOSHROOM) is a
   * coincidence Bukkit does not guarantee. On 1.20 those two constants were MUSHROOM_COW and
   * SNOWMAN, so name matching dropped both eggs in silence; 1.21 renamed them to MOOSHROOM and
   * SNOW_GOLEM, which would have broken a plugin that had adapted to the old names. The key,
   * minecraft:mooshroom, did not move either time.
   */
  private static final Map<String, EntityType> ENTITY_TYPES_BY_KEY = entityTypesByKey();

  private static Map<String, EntityType> entityTypesByKey() {
    Map<String, EntityType> byKey = new HashMap<>();
    for (EntityType type : EntityType.values()) {
      try {
        byKey.put(type.getKey().getKey(), type);
      } catch (IllegalArgumentException noKey) {
        // EntityType.UNKNOWN has no namespaced key.
      }
    }
    return byKey;
  }

  private EntityType eggEntityType(Material eggMaterial) {
    String eggKey = eggMaterial.getKey().getKey();
    if (!eggKey.endsWith("_spawn_egg")) {
      return null;
    }
    return ENTITY_TYPES_BY_KEY.get(eggKey.substring(0, eggKey.length() - "_spawn_egg".length()));
  }

  private List<EntityType> getSpawnEggsFromChest(Inventory inv) {
    List<EntityType> spawnEggs = new ArrayList<>();

    for (ItemStack item : inv.getContents()) {
      if (item == null || !item.getType().toString().endsWith("_SPAWN_EGG")) {
        continue;
      }
      EntityType entityType = eggEntityType(item.getType());
      if (entityType == null) {
        // Silently dropping the egg leaves the operator with a partially correct mob pool and
        // nothing to debug from.
        plugin.log(
            Level.WARNING,
            "Ignoring "
                + item.getType()
                + " in the wave chest: no entity type matches it on this server version");
        continue;
      }
      for (int i = 0; i < item.getAmount(); i++) {
        spawnEggs.add(entityType);
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
    MobTracker mobTracker = plugin.getMobTracker();

    // "random" draws a point per mob; "all" spawns the full amount at every point.
    boolean random = mode.equals("random");
    int total = random ? amount : amount * locations.size();
    plugin.log(
        Level.INFO,
        random
            ? "Spawning " + amount + " mobs across " + locations.size() + " random locations"
            : "Spawning at all " + locations.size() + " locations");

    int spawned = 0;
    int refused = 0;
    for (int n = 0; n < total; n++) {
      Location spawnLoc =
          random
              ? locations.get(spawnManager.getRandom().nextInt(locations.size()))
              : locations.get(n / amount);
      EntityType entityType = spawnEggs.get(spawnManager.getRandom().nextInt(spawnEggs.size()));
      Entity entity = spawnLoc.getWorld().spawnEntity(spawnLoc, entityType);

      // A cancelled CreatureSpawnEvent (region protection, anti-lag plugins) still hands back
      // the entity object, so counting unconditionally reports mobs that do not exist.
      if (entity == null || !entity.isValid()) {
        refused++;
        continue;
      }

      if (entity instanceof LivingEntity living) {
        mobTracker.tagMob(living, groupName, waveName);
      }
      spawned++;
    }

    if (refused > 0) {
      plugin.log(
          Level.WARNING,
          refused
              + " of "
              + (spawned + refused)
              + " mobs were refused by the server, most likely a protection plugin cancelling"
              + " the spawn");
    }

    return spawned;
  }
}
