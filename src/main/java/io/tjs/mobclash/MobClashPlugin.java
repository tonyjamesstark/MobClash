package io.tjs.mobclash;

import io.tjs.mobclash.commands.*;
import io.tjs.mobclash.listeners.MobDeathListener;
import io.tjs.mobclash.managers.KillBoard;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class MobClashPlugin extends JavaPlugin {

  /** config.yml's migration steps, see {@link FileFormat#upgrade}. */
  static final List<Consumer<ConfigurationSection>> CONFIG_FORMAT = List.of(FileFormat.STAMP);

  private SpawnManager spawnManager;
  private LanguageManager languageManager;
  private MobTracker mobTracker;
  private KillBoard killBoard;
  private Level loggingLevel;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    upgradeConfig();
    loadLoggingLevel();

    log(Level.INFO, "Initializing MobClash plugin...");

    languageManager = new LanguageManager(this);
    log(Level.INFO, "Language manager loaded");

    DataFile spawns = new DataFile(this, "spawns.yml", SpawnManager.FORMAT);
    DataFile kills = new DataFile(this, "kills.yml", MobTracker.FORMAT);
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

  /**
   * Re-read config.yml and language.yml from disk. Spawn and kill data are left alone: the plugin
   * owns those files and holds the live copy in memory.
   *
   * <p>Both files are parsed first and nothing changes unless both parse. Bukkit's own loaders log
   * a broken file and fall back to the defaults, which would silently discard every setting and
   * message.
   *
   * @throws InvalidConfigurationException whose message is the name of the file that does not parse
   */
  public void reloadSettings() throws InvalidConfigurationException {
    for (String name : List.of("config.yml", "language.yml")) {
      requireParses(new File(getDataFolder(), name));
    }
    reloadConfig();
    upgradeConfig();
    loadLoggingLevel();
    languageManager.reload();
  }

  /**
   * Throw if the file exists and is not valid YAML, logging the parser's error. A missing file
   * passes: reloading falls back to the bundled copy, as startup does.
   */
  static void requireParses(File file) throws InvalidConfigurationException {
    if (!file.exists()) {
      return;
    }
    try {
      new YamlConfiguration().load(file);
    } catch (IOException | InvalidConfigurationException e) {
      throw new InvalidConfigurationException(file.getName(), e);
    }
  }

  /** Bring the server's config.yml to the current format, writing it back if that changed it. */
  private void upgradeConfig() {
    if (FileFormat.upgrade(getConfig(), "config.yml", CONFIG_FORMAT, getLogger())) {
      saveConfig();
    }
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
    executors.put("removegroup", new RemoveGroupCommand(this, spawnManager, languageManager));
    executors.put("listgroups", new ListGroupsCommand(this, spawnManager, languageManager));
    executors.put("listspawns", new ListSpawnsCommand(this, spawnManager, languageManager));
    executors.put("showspawns", new ShowSpawnsCommand(this, spawnManager, languageManager));
    executors.put("setchest", new SetChestCommand(this, spawnManager, languageManager));
    executors.put("listchests", new ListChestsCommand(this, spawnManager, languageManager));
    executors.put("removechest", new RemoveChestCommand(this, spawnManager, languageManager));
    executors.put("summonmobs", new SummonMobsCommand(this, spawnManager, languageManager));
    executors.put("killmobs", new KillMobsCommand(this, spawnManager, languageManager, mobTracker));
    executors.put(
        "kills", new KillsCommand(this, spawnManager, languageManager, mobTracker, killBoard));
    executors.put(
        "killboard", new KillBoardCommand(this, spawnManager, languageManager, killBoard));

    executors.forEach(
        (name, executor) -> {
          PluginCommand command = declaredCommand(name);
          if (command == null) {
            return;
          }
          command.setExecutor(executor);
          // Set here rather than in plugin.yml so it cannot drift from the node the executor
          // checks. Bukkit then hides the command from anyone who lacks it.
          command.setPermission(executor.getPermission());
          executor.setUsage(command.getUsage());
          if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
          }
        });

    // /mobclash has no permission of its own: help lists only what the sender may run.
    Map<String, BaseCommand> subcommands = new LinkedHashMap<>(executors);
    Map<String, BaseCommand> ownOnly = new LinkedHashMap<>();
    ownOnly.put("reload", new ReloadCommand(this, spawnManager, languageManager));
    ownOnly.put("version", new VersionCommand(this, spawnManager, languageManager));
    subcommands.putAll(ownOnly);
    MobClashCommand root = new MobClashCommand(languageManager, subcommands);
    PluginCommand command = declaredCommand("mobclash");
    if (command != null) {
      command.setExecutor(root);
      command.setTabCompleter(root);
      ownOnly.forEach((name, sub) -> sub.setUsage(subcommandUsage(command.getUsage(), name)));
    }
  }

  /**
   * The lines of /mobclash's usage for one of the subcommands that exist only under it, such as
   * {@code /<command> reload - ...}, rewritten as that subcommand's own usage, {@code /<command> -
   * ...}.
   */
  static String subcommandUsage(String rootUsage, String name) {
    String prefix = "/<command> " + name;
    return rootUsage
        .lines()
        .filter(line -> line.equals(prefix) || line.startsWith(prefix + " "))
        .map(line -> "/<command>" + line.substring(prefix.length()))
        .collect(Collectors.joining("\n"));
  }

  /**
   * The plugin.yml command of that name, or null with a log line. getCommand returns null for
   * anything missing from plugin.yml; without this the failure is a bare NPE that does not say
   * which command drifted.
   */
  private PluginCommand declaredCommand(String name) {
    PluginCommand command = getCommand(name);
    if (command == null) {
      getLogger().severe("Command '" + name + "' is missing from plugin.yml, not registered");
    } else {
      log(Level.INFO, "Registering command: " + name);
    }
    return command;
  }

  private void registerListeners() {
    getServer()
        .getPluginManager()
        .registerEvents(new MobDeathListener(this, mobTracker, killBoard, languageManager), this);
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
