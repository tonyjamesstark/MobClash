package io.tjs.mobclash.managers;

import io.tjs.mobclash.DataFile;
import io.tjs.mobclash.MobClashPlugin;
import java.util.*;
import java.util.logging.Level;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class MobTracker {

  private final MobClashPlugin plugin;
  private final DataFile storage;
  private final NamespacedKey mobclashKey;
  private final Map<UUID, Integer> playerKills;

  public MobTracker(MobClashPlugin plugin, DataFile storage) {
    this.plugin = plugin;
    this.storage = storage;
    // Namespace written out rather than taken from the plugin. NamespacedKey(Plugin, String)
    // derived it from getName() on 1.20 and from namespace() on 1.21; if those ever differ,
    // every mob tagged by an older build stops being recognised and silently drops out of
    // kill tracking. "mobclash" is what 1.20 produced, so pinning it keeps old tags valid.
    this.mobclashKey = new NamespacedKey("mobclash", "mobclash_spawned");
    this.playerKills = new HashMap<>();
    loadKillData();
  }

  /** Tag a mob as spawned by MobClash */
  public void tagMob(LivingEntity entity, String groupName, String waveName) {
    String tag = groupName + ":" + waveName;
    entity.getPersistentDataContainer().set(mobclashKey, PersistentDataType.STRING, tag);
    plugin.log(Level.INFO, "Tagged mob " + entity.getType() + " with: " + tag);
  }

  /** Check if a mob was spawned by MobClash */
  public boolean isMobClashMob(Entity entity) {
    if (!(entity instanceof LivingEntity)) {
      return false;
    }
    return entity.getPersistentDataContainer().has(mobclashKey, PersistentDataType.STRING);
  }

  /** Get the spawn info for a MobClash mob */
  public String getMobSpawnInfo(Entity entity) {
    if (!isMobClashMob(entity)) {
      return null;
    }
    return entity.getPersistentDataContainer().get(mobclashKey, PersistentDataType.STRING);
  }

  /** Record a kill for a player */
  public void recordKill(Player player) {
    UUID uuid = player.getUniqueId();
    playerKills.put(uuid, playerKills.getOrDefault(uuid, 0) + 1);
    plugin.log(
        Level.INFO, player.getName() + " killed MobClash mob. Total: " + playerKills.get(uuid));
  }

  /** Get kill count for a player */
  public int getKills(Player player) {
    return playerKills.getOrDefault(player.getUniqueId(), 0);
  }

  /** Get kill count by UUID */
  public int getKills(UUID uuid) {
    return playerKills.getOrDefault(uuid, 0);
  }

  /** Get all player kills sorted by count */
  public List<Map.Entry<UUID, Integer>> getTopKills(int limit) {
    return playerKills.entrySet().stream()
        .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
        .limit(limit)
        .toList();
  }

  /** Reset kills for a player */
  public void resetKills(Player player) {
    playerKills.remove(player.getUniqueId());
    plugin.log(Level.INFO, "Reset kills for " + player.getName());
  }

  /** Reset all kills */
  public void resetAllKills() {
    playerKills.clear();
    plugin.log(Level.INFO, "Reset all player kills");
  }

  /** Load kill data from the tracking file */
  private void loadKillData() {
    plugin.log(Level.INFO, "Loading kill tracking data...");
    // contains() is true for a scalar at this path too, so a hand edit or a crash mid-write
    // would previously NPE out of the constructor and fail onEnable.
    ConfigurationSection section = storage.config().getConfigurationSection("player-kills");
    if (section != null) {
      for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
        try {
          UUID uuid = UUID.fromString(entry.getKey());
          // YAML hands back Integer or Long depending on magnitude.
          int count = ((Number) entry.getValue()).intValue();
          playerKills.put(uuid, count);
        } catch (IllegalArgumentException | ClassCastException | NullPointerException e) {
          plugin.log(
              Level.WARNING,
              "Discarding unreadable kill data for '"
                  + entry.getKey()
                  + "' (value: "
                  + entry.getValue()
                  + ")");
        }
      }
      plugin.log(Level.INFO, "Loaded kill data for " + playerKills.size() + " players");
    }
  }

  /** Save kill data to the tracking file */
  public void saveKillData() {
    plugin.log(Level.INFO, "Saving kill tracking data...");
    storage.config().set("player-kills", null);
    for (Map.Entry<UUID, Integer> entry : playerKills.entrySet()) {
      storage.config().set("player-kills." + entry.getKey().toString(), entry.getValue());
    }
    storage.save();
    plugin.log(Level.INFO, "Saved kill data for " + playerKills.size() + " players");
  }
}
