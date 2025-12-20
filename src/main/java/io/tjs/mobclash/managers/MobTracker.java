package io.tjs.mobclash.managers;

import io.tjs.mobclash.MobClashPlugin;
import java.util.*;
import java.util.logging.Level;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class MobTracker {

  private final MobClashPlugin plugin;
  private final NamespacedKey mobclashKey;
  private final Map<UUID, Integer> playerKills;

  public MobTracker(MobClashPlugin plugin) {
    this.plugin = plugin;
    this.mobclashKey = new NamespacedKey(plugin, "mobclash_spawned");
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

  /** Load kill data from config */
  private void loadKillData() {
    plugin.log(Level.INFO, "Loading kill tracking data...");
    if (plugin.getConfig().contains("player-kills")) {
      Map<String, Object> kills =
          plugin.getConfig().getConfigurationSection("player-kills").getValues(false);
      for (Map.Entry<String, Object> entry : kills.entrySet()) {
        try {
          UUID uuid = UUID.fromString(entry.getKey());
          int count = (Integer) entry.getValue();
          playerKills.put(uuid, count);
        } catch (Exception e) {
          plugin.log(Level.INFO, "Failed to load kill data for: " + entry.getKey());
        }
      }
      plugin.log(Level.INFO, "Loaded kill data for " + playerKills.size() + " players");
    }
  }

  /** Save kill data to config */
  public void saveKillData() {
    plugin.log(Level.INFO, "Saving kill tracking data...");
    plugin.getConfig().set("player-kills", null);
    for (Map.Entry<UUID, Integer> entry : playerKills.entrySet()) {
      plugin.getConfig().set("player-kills." + entry.getKey().toString(), entry.getValue());
    }
    plugin.saveConfig();
    plugin.log(Level.INFO, "Saved kill data for " + playerKills.size() + " players");
  }
}
