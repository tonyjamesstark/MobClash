package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.RandomEquipment;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;

public class SummonMobsCommand extends BaseCommand {

  /**
   * One egg's worth of a wave's mob pool. {@code data} is the egg's entity_data, null for a plain
   * egg; it holds whatever the egg was given -- attributes, Health, DeathLootTable,
   * PersistenceRequired -- which spawning by type alone discards.
   */
  private record PoolEntry(EntityType type, EntitySnapshot data) {
    Entity spawn(Location location) {
      if (data == null) {
        return location.getWorld().spawnEntity(location, type);
      }
      Entity entity = data.createEntity(location);
      // Gear the egg gives is part of the mob, not loot: vanilla drops it 8.5% of the time, or
      // always under an explicit drop_chances. Anything the mob picks up later still drops, since
      // picking up sets that slot's chance again.
      if (entity instanceof Mob mob) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
          mob.getEquipment().setDropChance(slot, 0f);
        }
      }
      return entity;
    }
  }

  /**
   * Keys in the SNBT that EntitySnapshot.getAsString writes, which leaves simple keys unquoted.
   * Passengers would spawn untracked riders past max-mobs-per-summon; a fixed UUID lets only the
   * first copy into the world.
   */
  // ponytail: matches the key at any depth, so an item inside the egg data that carries its own
  // UUID key is refused too. Parse the SNBT and check the top level only if that ever bites.
  private static final Pattern RIDERS = Pattern.compile("[{,]Passengers:");

  private static final Pattern FIXED_UUID = Pattern.compile("[{,]UUID:");

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

    Options options = parseOptions(sender, groupName, args);
    if (options == null) {
      return true;
    }
    int amount = options.amount();

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
    List<PoolEntry> spawnEggs = getSpawnEggsFromChest(sender, chest.getInventory());

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
    if (options.gear() != null) {
      plugin.log(Level.INFO, "Random equipment from " + options.gearSource());
    }

    int spawned = summonMobs(mode, locations, spawnEggs, amount, groupName, waveName, options);

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

  /**
   * The trailing arguments. {@code gear} is null when equipment is off; {@code gearSource} names
   * where it came from, for the log.
   */
  private record Options(int amount, RandomEquipment.Pools<ItemStack> gear, String gearSource) {}

  /**
   * Parse {@code [amount] [true|false|<gear chest>]}, either one optional. Returns null after
   * telling the sender what is wrong.
   */
  private Options parseOptions(CommandSender sender, String groupName, String[] args) {
    int next = 3;
    int amount = 1;
    if (args.length > next && args[next].matches("-?\\d+")) {
      try {
        amount = Integer.parseInt(args[next++]);
      } catch (NumberFormatException e) {
        sender.sendMessage(langManager.getMessage("invalid-number"));
        return null;
      }
      if (amount < 1 || amount > 100) {
        sender.sendMessage(langManager.getMessage("invalid-amount"));
        return null;
      }
    }
    String equipment = args.length > next ? args[next++] : "false";
    if (args.length > next) {
      sender.sendMessage(langManager.getMessage("summonmobs-usage"));
      return null;
    }

    if (equipment.equalsIgnoreCase("false")) {
      return new Options(amount, null, null);
    }
    if (equipment.equalsIgnoreCase("true")) {
      return new Options(amount, configGear(), "config.yml");
    }
    Location gearChest =
        SpawnManager.isValidName(equipment)
            ? spawnManager.getGroupChest(groupName, equipment)
            : null;
    if (gearChest == null) {
      sender.sendMessage(langManager.getMessage("invalid-equipment", equipment, groupName));
      return null;
    }
    Block block = gearChest.getBlock();
    if (block.getType() != Material.CHEST) {
      sender.sendMessage(langManager.getMessage("chest-missing"));
      return null;
    }
    return new Options(
        amount,
        RandomEquipment.fromChest(((Chest) block.getState()).getInventory()),
        "chest '" + equipment + "'");
  }

  private RandomEquipment.Pools<ItemStack> configGear() {
    return RandomEquipment.fromConfig(
            plugin.getConfig(), warning -> plugin.log(Level.WARNING, warning))
        .map(ItemStack::of);
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

  private List<PoolEntry> getSpawnEggsFromChest(CommandSender sender, Inventory inv) {
    List<PoolEntry> spawnEggs = new ArrayList<>();

    for (ItemStack item : inv.getContents()) {
      if (item == null || !item.getType().toString().endsWith("_SPAWN_EGG")) {
        continue;
      }
      EntitySnapshot data =
          item.getItemMeta() instanceof SpawnEggMeta meta ? meta.getSpawnedEntity() : null;
      EntityType entityType = data != null ? data.getEntityType() : eggEntityType(item.getType());
      if (entityType == null) {
        skipEgg(
            sender, item, "egg-unknown-type", "no entity type matches it on this server version");
        continue;
      }
      if (data != null) {
        // Vanilla refuses entity data for block and minecart types unless an op places the egg;
        // here anyone with access to the chest could plant one. Only mobs are tagged and counted.
        Class<? extends Entity> entityClass = entityType.getEntityClass();
        if (entityClass == null || !Mob.class.isAssignableFrom(entityClass)) {
          skipEgg(
              sender,
              item,
              "egg-not-a-mob",
              "its entity data spawns " + entityType + ", not a mob");
          continue;
        }
        String snbt = data.getAsString();
        if (RIDERS.matcher(snbt).find()) {
          skipEgg(sender, item, "egg-has-riders", "its entity data has Passengers");
          continue;
        }
        if (FIXED_UUID.matcher(snbt).find()) {
          skipEgg(sender, item, "egg-has-uuid", "its entity data fixes a UUID");
          continue;
        }
      }
      PoolEntry entry = new PoolEntry(entityType, data);
      for (int i = 0; i < item.getAmount(); i++) {
        spawnEggs.add(entry);
      }
    }

    return spawnEggs;
  }

  /**
   * Leave an egg out of the pool, telling the console and whoever ran the summon -- a player, or a
   * command block's last output -- why. A silently shorter pool leaves nothing to debug from.
   */
  private void skipEgg(CommandSender sender, ItemStack item, String key, String reason) {
    plugin.log(Level.WARNING, "Ignoring " + item.getType() + " in the wave chest: " + reason);
    if (!(sender instanceof ConsoleCommandSender)) {
      sender.sendMessage(langManager.getMessage(key, item.getType()));
    }
  }

  private String getUniqueMobTypes(List<PoolEntry> spawnEggs) {
    return spawnEggs.stream()
        .map(entry -> entry.type().name())
        .distinct()
        .reduce((a, b) -> a + ", " + b)
        .orElse("none");
  }

  private int summonMobs(
      String mode,
      List<Location> locations,
      List<PoolEntry> spawnEggs,
      int amount,
      String groupName,
      String waveName,
      Options options) {
    MobTracker mobTracker = plugin.getMobTracker();
    boolean equip = options.gear() != null;
    int maxProtection = equip ? plugin.getConfig().getInt("random-equipment.max-protection", 4) : 0;
    int maxSharpness = equip ? plugin.getConfig().getInt("random-equipment.max-sharpness", 5) : 0;

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
      PoolEntry entry = spawnEggs.get(spawnManager.getRandom().nextInt(spawnEggs.size()));
      Entity entity = entry.spawn(spawnLoc);

      // A cancelled CreatureSpawnEvent (region protection, anti-lag plugins) still hands back
      // the entity object, so counting unconditionally reports mobs that do not exist.
      if (entity == null || !entity.isValid()) {
        refused++;
        continue;
      }

      RandomEquipment.Kit kit = equip ? RandomEquipment.KITS.get(entity.getType()) : null;
      if (kit != null && entity instanceof Mob mob) {
        RandomEquipment.equip(
            mob,
            kit,
            RandomEquipment.roll(
                kit, options.gear(), maxProtection, maxSharpness, spawnManager.getRandom()),
            entry.data() != null);
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
