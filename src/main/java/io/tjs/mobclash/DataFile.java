package io.tjs.mobclash;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * A YAML file of machine-written state under the plugin's data folder, separate from config.yml.
 *
 * <p>Spawn points and kill counts used to live in config.yml, so every {@code /addspawn} rewrote
 * the operator's settings file from the copy the plugin held in memory. An admin who edited
 * config.yml on a running server lost that edit at the next command. Keeping the two apart also
 * means a failure writing one cannot take the other with it.
 */
public class DataFile {

  private final MobClashPlugin plugin;
  private final File file;
  private final FileConfiguration config;

  public DataFile(MobClashPlugin plugin, String name) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), name);
    // Returns an empty configuration for a file that does not exist yet, which is what a first
    // run looks like.
    this.config = YamlConfiguration.loadConfiguration(file);
  }

  public FileConfiguration config() {
    return config;
  }

  public String name() {
    return file.getName();
  }

  /** Write the file, logging rather than throwing: callers are save paths, including onDisable. */
  public void save() {
    try {
      File parent = file.getParentFile();
      if (parent != null) {
        parent.mkdirs();
      }
      config.save(file);
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Could not write " + file, e);
    }
  }

  /**
   * Move one section out of a legacy configuration into this file. Returns whether anything moved,
   * so the caller can decide whether the source is worth rewriting.
   */
  public boolean adopt(FileConfiguration legacy, String path) {
    ConfigurationSection section = legacy.getConfigurationSection(path);
    if (section == null) {
      return false;
    }
    config.set(path, null);
    // Deep values are dotted paths to leaves plus the sections in between; setting only the
    // leaves rebuilds the same tree here.
    for (Map.Entry<String, Object> entry : section.getValues(true).entrySet()) {
      if (!(entry.getValue() instanceof ConfigurationSection)) {
        config.set(path + "." + entry.getKey(), entry.getValue());
      }
    }
    legacy.set(path, null);
    return true;
  }
}
