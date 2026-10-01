package io.tjs.mobclash.managers;

import io.tjs.mobclash.DataFile;
import io.tjs.mobclash.FileFormat;
import io.tjs.mobclash.MobClashPlugin;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class SpawnManager {

  /**
   * Group and wave names are used as configuration keys. Bukkit treats '.' as a path separator, so
   * a name containing one would nest instead of creating a single key and would not survive a
   * reload.
   */
  private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

  /** spawns.yml's migration steps, see {@link FileFormat#upgrade}. */
  public static final List<Consumer<ConfigurationSection>> FORMAT = List.of(FileFormat.STAMP);

  private final MobClashPlugin plugin;
  private final DataFile storage;
  private final Map<String, List<Location>> spawnGroups;
  private final Map<String, Map<String, Location>>
      groupChests; // group -> (wave name -> chest location)
  private final Random random;

  public SpawnManager(MobClashPlugin plugin, DataFile storage) {
    this.plugin = plugin;
    this.storage = storage;
    this.spawnGroups = new HashMap<>();
    this.groupChests = new HashMap<>();
    this.random = new Random();
    loadConfigData();
  }

  /** Whether a group or wave name is safe to use as a configuration key. */
  public static boolean isValidName(String name) {
    return name != null && VALID_NAME.matcher(name).matches();
  }

  /**
   * Read one location from a config section, or null when it cannot be resolved. A world that is
   * missing or not loaded yields a warning and a skip rather than a Location with a null world,
   * which would throw at the first use and corrupt the next save.
   */
  private Location readLocation(ConfigurationSection section, String description) {
    String worldName = section.getString("world");
    if (worldName == null) {
      plugin.log(Level.WARNING, "Skipping " + description + ": no world recorded");
      return null;
    }
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
      plugin.log(
          Level.WARNING, "Skipping " + description + ": world '" + worldName + "' is not loaded");
      return null;
    }
    return new Location(
        world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"));
  }

  /**
   * The location's world name, or null if it was never set or has since been unloaded. Paper's
   * {@link Location#getWorld()} throws for an unloaded world rather than returning null.
   */
  public static String worldNameOf(Location location) {
    try {
      World world = location.getWorld();
      return world == null ? null : world.getName();
    } catch (IllegalArgumentException worldUnloaded) {
      return null;
    }
  }

  /**
   * The four config values for one location, or null when its world is gone. Returning the values
   * instead of writing them lets the caller stage a whole save before touching the live config.
   */
  private Map<String, Object> locationValues(Location location, String description) {
    String worldName = worldNameOf(location);
    if (worldName == null) {
      plugin.log(Level.WARNING, "Not saving " + description + ": its world is no longer loaded");
      return null;
    }
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("world", worldName);
    values.put("x", location.getX());
    values.put("y", location.getY());
    values.put("z", location.getZ());
    return values;
  }

  private void loadConfigData() {
    plugin.log(Level.INFO, "Loading spawn configuration from " + storage.name() + "...");
    FileConfiguration config = storage.config();

    // Load spawn groups
    ConfigurationSection groupsSection = config.getConfigurationSection("spawn-groups");
    if (groupsSection != null) {
      for (String groupName : groupsSection.getKeys(false)) {
        if (!isValidName(groupName)) {
          plugin.log(Level.WARNING, "Skipping group '" + groupName + "': invalid name");
          continue;
        }
        List<Location> locations = new ArrayList<>();
        ConfigurationSection groupSection = groupsSection.getConfigurationSection(groupName);
        if (groupSection != null) {
          for (String key : groupSection.getKeys(false)) {
            ConfigurationSection locSection = groupSection.getConfigurationSection(key);
            if (locSection != null) {
              Location loc =
                  readLocation(locSection, "spawn point '" + groupName + "." + key + "'");
              if (loc != null) {
                locations.add(loc);
              }
            }
          }
        }
        spawnGroups.put(groupName, locations);
        plugin.log(
            Level.INFO,
            "Loaded group '" + groupName + "' with " + locations.size() + " spawn points");
      }
    }

    // Load group chests
    ConfigurationSection chestsSection = config.getConfigurationSection("group-chests");
    if (chestsSection != null) {
      for (String groupName : chestsSection.getKeys(false)) {
        if (!isValidName(groupName)) {
          plugin.log(Level.WARNING, "Skipping chests for group '" + groupName + "': invalid name");
          continue;
        }
        ConfigurationSection groupChestsSection = chestsSection.getConfigurationSection(groupName);
        if (groupChestsSection != null) {
          Map<String, Location> waves = new HashMap<>();
          for (String waveName : groupChestsSection.getKeys(false)) {
            if (!isValidName(waveName)) {
              plugin.log(
                  Level.WARNING,
                  "Skipping wave '" + waveName + "' of group '" + groupName + "': invalid name");
              continue;
            }
            ConfigurationSection chestSection =
                groupChestsSection.getConfigurationSection(waveName);
            if (chestSection != null) {
              Location loc =
                  readLocation(
                      chestSection, "chest '" + waveName + "' of group '" + groupName + "'");
              if (loc != null) {
                waves.put(waveName, loc);
                plugin.log(
                    Level.INFO,
                    "Loaded chest '"
                        + waveName
                        + "' for group '"
                        + groupName
                        + "' in "
                        + worldNameOf(loc)
                        + " at "
                        + String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ()));
              }
            }
          }
          groupChests.put(groupName, waves);
        }
      }
    }

    plugin.log(
        Level.INFO,
        "Configuration loaded: "
            + spawnGroups.size()
            + " groups, "
            + groupChests.values().stream().mapToInt(Map::size).sum()
            + " chests");
  }

  /**
   * Serialize every group and chest, then write. The whole tree is staged first so that a location
   * we cannot serialize cannot leave the config half-erased: nothing is cleared until every value
   * is in hand.
   */
  public void saveConfigData() {
    plugin.log(Level.INFO, "Saving spawn configuration to " + storage.name() + "...");
    FileConfiguration config = storage.config();

    Map<String, Map<String, Object>> staged = new LinkedHashMap<>();

    for (Map.Entry<String, List<Location>> entry : spawnGroups.entrySet()) {
      String groupName = entry.getKey();
      List<Location> locations = entry.getValue();

      int saved = 0;
      for (Location loc : locations) {
        Map<String, Object> values =
            locationValues(loc, "a spawn point of group '" + groupName + "'");
        if (values == null) {
          continue;
        }
        staged.put("spawn-groups." + groupName + ".point-" + saved, values);
        saved++;
      }
      plugin.log(Level.INFO, "Saved " + saved + " spawn points for group '" + groupName + "'");
    }

    for (Map.Entry<String, Map<String, Location>> groupEntry : groupChests.entrySet()) {
      String groupName = groupEntry.getKey();

      for (Map.Entry<String, Location> waveEntry : groupEntry.getValue().entrySet()) {
        String waveName = waveEntry.getKey();
        Map<String, Object> values =
            locationValues(
                waveEntry.getValue(), "chest '" + waveName + "' of group '" + groupName + "'");
        if (values == null) {
          continue;
        }
        staged.put("group-chests." + groupName + "." + waveName, values);
        plugin.log(Level.INFO, "Saved chest '" + waveName + "' for group '" + groupName + "'");
      }
    }

    config.set("spawn-groups", null);
    config.set("group-chests", null);
    staged.forEach(
        (path, values) -> values.forEach((key, value) -> config.set(path + "." + key, value)));

    storage.save();
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
                location.getX(), location.getY(), location.getZ(), worldNameOf(location)));
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

    // Location.distance throws when the two worlds differ, so only compare within one world.
    String callerWorld = worldNameOf(playerLocation);
    for (Location loc : locations) {
      if (callerWorld == null || !callerWorld.equals(worldNameOf(loc))) {
        continue;
      }
      double dist = loc.distance(playerLocation);
      if (dist < minDist) {
        minDist = dist;
        nearest = loc;
      }
    }

    if (nearest == null) {
      plugin.log(
          Level.INFO,
          "No spawn point of group '" + groupName + "' is in the caller's world; removed nothing");
      return false;
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
    dropIfEmptyAndSave(groupName, locations);
    return true;
  }

  /**
   * Remove the spawn point at {@code index} in {@link #getSpawnPoints} order, which is the order
   * /listspawns numbers from 1. The points after it move up one place.
   *
   * @return the removed point, or null when the group has no point at that index
   */
  public Location removeSpawnPoint(String groupName, int index) {
    List<Location> locations = spawnGroups.get(groupName);
    if (locations == null || index < 0 || index >= locations.size()) {
      return null;
    }
    Location removed = locations.remove(index);
    plugin.log(
        Level.INFO,
        "Removed spawn point #"
            + (index + 1)
            + " from group '"
            + groupName
            + "' at "
            + String.format(
                "(%.1f, %.1f, %.1f) in %s",
                removed.getX(), removed.getY(), removed.getZ(), worldNameOf(removed)));
    dropIfEmptyAndSave(groupName, locations);
    return removed;
  }

  /** The tail of every spawn point removal: a group with no points left goes too. */
  private void dropIfEmptyAndSave(String groupName, List<Location> locations) {
    if (locations.isEmpty()) {
      spawnGroups.remove(groupName);
      plugin.log(Level.INFO, "Group '" + groupName + "' is now empty and has been removed");
    }
    saveConfigData();
  }

  public void setGroupChest(String groupName, String waveName, Location chestLocation) {
    groupChests.putIfAbsent(groupName, new HashMap<>());
    groupChests.get(groupName).put(waveName, chestLocation);
    plugin.log(
        Level.INFO,
        "Set chest '"
            + waveName
            + "' for group '"
            + groupName
            + "' at "
            + String.format(
                "(%.1f, %.1f, %.1f) in %s",
                chestLocation.getX(),
                chestLocation.getY(),
                chestLocation.getZ(),
                worldNameOf(chestLocation)));
    saveConfigData();
  }

  /**
   * Forget a chest set with /setchest. The block in the world is not touched.
   *
   * @return whether the group had a chest of that name
   */
  public boolean removeGroupChest(String groupName, String chestName) {
    Map<String, Location> chests = groupChests.get(groupName);
    if (chests == null || chests.remove(chestName) == null) {
      return false;
    }
    if (chests.isEmpty()) {
      groupChests.remove(groupName);
    }
    plugin.log(Level.INFO, "Removed chest '" + chestName + "' from group '" + groupName + "'");
    saveConfigData();
    return true;
  }

  /**
   * Forget every spawn point and chest of a group. Chest blocks in the world are not touched.
   *
   * @return whether the group had any spawn points or chests
   */
  public boolean removeGroup(String groupName) {
    List<Location> points = spawnGroups.remove(groupName);
    Map<String, Location> chests = groupChests.remove(groupName);
    if (points == null && chests == null) {
      return false;
    }
    plugin.log(
        Level.INFO,
        "Removed group '"
            + groupName
            + "' with "
            + (points == null ? 0 : points.size())
            + " spawn points and "
            + (chests == null ? 0 : chests.size())
            + " chests");
    saveConfigData();
    return true;
  }

  public boolean hasGroup(String groupName) {
    return spawnGroups.containsKey(groupName);
  }

  public List<Location> getSpawnPoints(String groupName) {
    return List.copyOf(spawnGroups.getOrDefault(groupName, List.of()));
  }

  public Location getGroupChest(String groupName, String waveName) {
    Map<String, Location> waves = groupChests.get(groupName);
    if (waves == null) {
      return null;
    }
    return waves.get(waveName);
  }

  public Map<String, Location> getGroupWaves(String groupName) {
    return Map.copyOf(groupChests.getOrDefault(groupName, Map.of()));
  }

  /**
   * The groups that have at least one chest set with /setchest. A group can have chests but no
   * spawn points, so it would not appear in {@link #getAllGroups()}.
   */
  public Set<String> getChestGroups() {
    return Set.copyOf(groupChests.keySet());
  }

  public Map<String, List<Location>> getAllGroups() {
    Map<String, List<Location>> copy = new HashMap<>();
    spawnGroups.forEach((name, locations) -> copy.put(name, List.copyOf(locations)));
    return Map.copyOf(copy);
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
