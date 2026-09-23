package io.tjs.mobclash;

import io.tjs.mobclash.commands.*;
import io.tjs.mobclash.listeners.MobDeathListener;
import io.tjs.mobclash.managers.KillBoard;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MobClashPlugin extends JavaPlugin {

  private SpawnManager spawnManager;
  private LanguageManager languageManager;
  private MobTracker mobTracker;
  private KillBoard killBoard;
  private Level loggingLevel;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    loadLoggingLevel();

    log(Level.INFO, "Initializing MobClash plugin...");

    languageManager = new LanguageManager(this);
    log(Level.INFO, "Language manager loaded");

    DataFile spawns = new DataFile(this, "spawns.yml");
    DataFile kills = new DataFile(this, "kills.yml");
    migrateRuntimeStateOutOfConfig(spawns, kills);

    spawnManager = new SpawnManager(this, spawns);
    log(Level.INFO, "Spawn manager loaded with " + spawnManager.getAllGroups().size() + " groups");

    mobTracker = new MobTracker(this, kills);
    log(Level.INFO, "Mob tracker initialized");

    killBoard =
        new KillBoard(this, mobTracker, languageManager, getServer().getScoreboardManager());

    registerCommands();
    log(Level.INFO, "Commands registered");

    registerListeners();
    log(Level.INFO, "Event listeners registered");

    getLogger().info("MobClash plugin enabled successfully!");
  }

  @Override
  public void onDisable() {
    log(Level.INFO, "Disabling MobClash plugin...");

    // A reload would otherwise leave viewers holding a scoreboard nothing updates any more.
    if (killBoard != null) {
      killBoard.hideAll();
    }

    // Each save is isolated: a failure in one must not discard the other's data, and Bukkit
    // swallows anything thrown out of onDisable.
    if (spawnManager != null) {
      try {
        spawnManager.saveConfigData();
        log(Level.INFO, "Spawn configuration saved");
      } catch (RuntimeException e) {
        getLogger().log(Level.SEVERE, "Failed to save spawn configuration", e);
      }
    }

    if (mobTracker != null) {
      try {
        mobTracker.saveKillData();
        log(Level.INFO, "Kill tracking data saved");
      } catch (RuntimeException e) {
        getLogger().log(Level.SEVERE, "Failed to save kill tracking data", e);
      }
    }

    getLogger().info("MobClash plugin disabled!");
  }

  /**
   * Move spawn and kill data written by older versions out of config.yml, once. Runs before the
   * managers read their files, so an upgrade keeps its data and a fresh install does nothing.
   */
  private void migrateRuntimeStateOutOfConfig(DataFile spawns, DataFile kills) {
    boolean moved = spawns.adopt(getConfig(), "spawn-groups");
    moved |= spawns.adopt(getConfig(), "group-chests");
    moved |= kills.adopt(getConfig(), "player-kills");
    if (!moved) {
      return;
    }
    spawns.save();
    kills.save();
    saveConfig();
    getLogger().info("Moved spawn and kill data out of config.yml into spawns.yml and kills.yml");
  }

  private void loadLoggingLevel() {
    String levelStr = getConfig().getString("logging-level", "INFO").toUpperCase();
    try {
      loggingLevel = Level.parse(levelStr);
      getLogger().info("Logging level set to: " + loggingLevel.getName());
    } catch (IllegalArgumentException e) {
      loggingLevel = Level.INFO;
      getLogger().warning("Invalid logging level '" + levelStr + "', defaulting to INFO");
    }
  }

  private void registerCommands() {
    Map<String, BaseCommand> executors = new LinkedHashMap<>();
    executors.put("addspawn", new AddSpawnCommand(this, spawnManager, languageManager));
    executors.put("removespawn", new RemoveSpawnCommand(this, spawnManager, languageManager));
    executors.put("listgroups", new ListGroupsCommand(this, spawnManager, languageManager));
    executors.put("listspawns", new ListSpawnsCommand(this, spawnManager, languageManager));
    executors.put("showspawns", new ShowSpawnsCommand(this, spawnManager, languageManager));
    executors.put("setchest", new SetChestCommand(this, spawnManager, languageManager));
    executors.put("summonmobs", new SummonMobsCommand(this, spawnManager, languageManager));
    executors.put(
        "kills", new KillsCommand(this, spawnManager, languageManager, mobTracker, killBoard));
    executors.put(
        "killboard", new KillBoardCommand(this, spawnManager, languageManager, killBoard));

    executors.forEach(
        (name, executor) -> {
          PluginCommand command = getCommand(name);
          // getCommand returns null for anything missing from plugin.yml; without this the
          // failure is a bare NPE that does not say which command drifted.
          if (command == null) {
            getLogger().severe("Command '" + name + "' is missing from plugin.yml, not registered");
            return;
          }
          log(Level.INFO, "Registering command: " + name);
          command.setExecutor(executor);
          // Set here rather than in plugin.yml so it cannot drift from the node the executor
          // checks. Bukkit then hides the command from anyone who lacks it.
          command.setPermission(executor.getPermission());
        });
  }

  private void registerListeners() {
    getServer()
        .getPluginManager()
        .registerEvents(new MobDeathListener(this, mobTracker, killBoard), this);
    getServer().getPluginManager().registerEvents(killBoard, this);
  }

  public SpawnManager getSpawnManager() {
    return spawnManager;
  }

  public LanguageManager getLanguageManager() {
    return languageManager;
  }

  public MobTracker getMobTracker() {
    return mobTracker;
  }

  /** Log a message at the specified level if it meets the configured logging threshold */
  public void log(Level level, String message) {
    if (level.intValue() >= loggingLevel.intValue()) {
      getLogger().log(level, message);
    }
  }
}
