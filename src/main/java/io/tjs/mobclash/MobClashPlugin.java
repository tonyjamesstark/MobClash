package io.tjs.mobclash;

import io.tjs.mobclash.commands.*;
import io.tjs.mobclash.listeners.MobDeathListener;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public class MobClashPlugin extends JavaPlugin {

  private SpawnManager spawnManager;
  private LanguageManager languageManager;
  private MobTracker mobTracker;
  private Level loggingLevel;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    loadLoggingLevel();

    log(Level.INFO, "Initializing MobClash plugin...");

    languageManager = new LanguageManager(this);
    log(Level.INFO, "Language manager loaded");

    spawnManager = new SpawnManager(this);
    log(Level.INFO, "Spawn manager loaded with " + spawnManager.getAllGroups().size() + " groups");

    mobTracker = new MobTracker(this);
    log(Level.INFO, "Mob tracker initialized");

    registerCommands();
    log(Level.INFO, "Commands registered");

    registerListeners();
    log(Level.INFO, "Event listeners registered");

    getLogger().info("MobClash plugin enabled successfully!");
  }

  @Override
  public void onDisable() {
    log(Level.INFO, "Disabling MobClash plugin...");

    if (spawnManager != null) {
      spawnManager.saveConfigData();
      log(Level.INFO, "Spawn configuration saved");
    }

    if (mobTracker != null) {
      mobTracker.saveKillData();
      log(Level.INFO, "Kill tracking data saved");
    }

    getLogger().info("MobClash plugin disabled!");
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
    log(Level.INFO, "Registering command: addspawn");
    getCommand("addspawn").setExecutor(new AddSpawnCommand(this, spawnManager, languageManager));

    log(Level.INFO, "Registering command: removespawn");
    getCommand("removespawn")
        .setExecutor(new RemoveSpawnCommand(this, spawnManager, languageManager));

    log(Level.INFO, "Registering command: listgroups");
    getCommand("listgroups")
        .setExecutor(new ListGroupsCommand(this, spawnManager, languageManager));

    log(Level.INFO, "Registering command: listspawns");
    getCommand("listspawns")
        .setExecutor(new ListSpawnsCommand(this, spawnManager, languageManager));

    log(Level.INFO, "Registering command: showspawns");
    getCommand("showspawns")
        .setExecutor(new ShowSpawnsCommand(this, spawnManager, languageManager));

    log(Level.INFO, "Registering command: setchest");
    getCommand("setchest").setExecutor(new SetChestCommand(this, spawnManager, languageManager));

    log(Level.INFO, "Registering command: summonmobs");
    getCommand("summonmobs")
        .setExecutor(new SummonMobsCommand(this, spawnManager, languageManager));

    log(Level.INFO, "Registering command: kills");
    getCommand("kills")
        .setExecutor(new KillsCommand(this, spawnManager, languageManager, mobTracker));
  }

  private void registerListeners() {
    getServer().getPluginManager().registerEvents(new MobDeathListener(this, mobTracker), this);
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
