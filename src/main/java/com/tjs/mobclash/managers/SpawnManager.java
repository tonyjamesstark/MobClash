package com.example.mobclash.managers;

import com.example.mobclash.MobClashPlugin;
import java.util.*;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class SpawnManager {

  private final MobClashPlugin plugin;
  private final Map<String, List<Location>> spawnGroups;
  private final Map<String, Location> groupChests;
  private final Random random;

  public SpawnManager(MobClashPlugin plugin) {
    this.plugin = plugin;
    this.spawnGroups = new HashMap<>();
    this.groupChests = new HashMap<>();
    this.random = new Random();
    loadConfigData();
  }

  private void loadConfigData() {
    plugin.log(Level.INFO, "Loading spawn configuration...");
    FileConfiguration config = plugin.getConfig();

    // Load spawn groups
    if (config.contains("spawn-groups")) {
      ConfigurationSection groupsSection = config.getConfigurationSection("spawn-groups");
      if (groupsSection != null) {
        for (String groupName : groupsSection.getKeys(false)) {
          List<Location> locations = new ArrayList<>();
          ConfigurationSection groupSection = groupsSection.getConfigurationSection(groupName);
          if (groupSection != null) {
            for (String key : groupSection.getKeys(false)) {
              ConfigurationSection locSection = groupSection.getConfigurationSection(key);
              if (locSection != null) {
                Location loc =
                    new Location(
                        Bukkit.getWorld(locSection.getString("world")),
                        locSection.getDouble("x"),
                        locSection.getDouble("y"),
                        locSection.getDouble("z"));
                locations.add(loc);
              }
            }
          }
          spawnGroups.put(groupName, locations);
          plugin.log(
              Level.INFO,
              "Loaded group '" + groupName + "' with " + locations.size() + " spawn points");
        }
      }
    }

    // Load group chests
    if (config.contains("group-chests")) {
      ConfigurationSection chestsSection = config.getConfigurationSection("group-chests");
      if (chestsSection != null) {
        for (String groupName : chestsSection.getKeys(false)) {
          ConfigurationSection chestSection = chestsSection.getConfigurationSection(groupName);
          if (chestSection != null) {
            Location loc =
                new Location(
                    Bukkit.getWorld(chestSection.getString("world")),
                    chestSection.getDouble("x"),
                    chestSection.getDouble("y"),
                    chestSection.getDouble("z"));
            groupChests.put(groupName, loc);
            plugin.log(
                Level.INFO,
                "Loaded chest for group '"
                    + groupName
                    + "' at "
                    + String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ()));
          }
        }
      }
    }

    plugin.log(
        Level.INFO,
        "Configuration loaded: "
            + spawnGroups.size()
            + " groups, "
            + groupChests.size()
            + " chests");
  }

  public void saveConfigData() {
    plugin.log(Level.INFO, "Saving spawn configuration...");
    FileConfiguration config = plugin.getConfig();

    // Clear and save spawn groups
    config.set("spawn-groups", null);
    for (Map.Entry<String, List<Location>> entry : spawnGroups.entrySet()) {
      String groupName = entry.getKey();
      List<Location> locations = entry.getValue();

      for (int i = 0; i < locations.size(); i++) {
        Location loc = locations.get(i);
        String path = "spawn-groups." + groupName + ".point-" + i;
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
      }
      plugin.log(
          Level.INFO, "Saved " + locations.size() + " spawn points for group '" + groupName + "'");
    }

    // Save group chests
    config.set("group-chests", null);
    for (Map.Entry<String, Location> entry : groupChests.entrySet()) {
      String groupName = entry.getKey();
      Location loc = entry.getValue();
      String path = "group-chests." + groupName;
      config.set(path + ".world", loc.getWorld().getName());
      config.set(path + ".x", loc.getX());
      config.set(path + ".y", loc.getY());
      config.set(path + ".z", loc.getZ());
      plugin.log(Level.INFO, "Saved chest for group '" + groupName + "'");
    }

    plugin.saveConfig();
    plugin.log(Level.INFO, "Configuration saved successfully");
  }

  public void addSpawnPoint(String groupName, Location location) {
    spawnGroups.putIfAbsent(groupName, new ArrayList<>());
    spawnGroups.get(groupName).add(location);
    plugin.log(
        Level.INFO,
        "Added spawn point to group '"
            + groupName
            + "' at "
            + String.format(
                "(%.1f, %.1f, %.1f) in %s",
                location.getX(), location.getY(), location.getZ(), location.getWorld().getName()));
    saveConfigData();
  }

  public boolean removeNearestSpawnPoint(String groupName, Location playerLocation) {
    if (!spawnGroups.containsKey(groupName)) {
      plugin.log(
          Level.INFO, "Attempted to remove spawn from non-existent group '" + groupName + "'");
      return false;
    }

    List<Location> locations = spawnGroups.get(groupName);
    if (locations.isEmpty()) {
      return false;
    }

    Location nearest = null;
    double minDist = Double.MAX_VALUE;

    for (Location loc : locations) {
      double dist = loc.distance(playerLocation);
      if (dist < minDist) {
        minDist = dist;
        nearest = loc;
      }
    }

    locations.remove(nearest);
    plugin.log(
        Level.INFO,
        "Removed spawn point from group '"
            + groupName
            + "' at "
            + String.format(
                "(%.1f, %.1f, %.1f), %.1f blocks away",
                nearest.getX(), nearest.getY(), nearest.getZ(), minDist));

    if (locations.isEmpty()) {
      spawnGroups.remove(groupName);
      plugin.log(Level.INFO, "Group '" + groupName + "' is now empty and has been removed");
    }
    saveConfigData();
    return true;
  }

  public void setGroupChest(String groupName, Location chestLocation) {
    groupChests.put(groupName, chestLocation);
    plugin.log(
        Level.INFO,
        "Set chest for group '"
            + groupName
            + "' at "
            + String.format(
                "(%.1f, %.1f, %.1f) in %s",
                chestLocation.getX(),
                chestLocation.getY(),
                chestLocation.getZ(),
                chestLocation.getWorld().getName()));
    saveConfigData();
  }

  public boolean hasGroup(String groupName) {
    return spawnGroups.containsKey(groupName);
  }

  public List<Location> getSpawnPoints(String groupName) {
    return spawnGroups.getOrDefault(groupName, new ArrayList<>());
  }

  public Location getGroupChest(String groupName) {
    return groupChests.get(groupName);
  }

  public Map<String, List<Location>> getAllGroups() {
    return new HashMap<>(spawnGroups);
  }

  public Location getRandomSpawnPoint(String groupName) {
    List<Location> locations = spawnGroups.get(groupName);
    if (locations == null || locations.isEmpty()) {
      return null;
    }
    return locations.get(random.nextInt(locations.size()));
  }

  public Random getRandom() {
    return random;
  }
}
