package io.tjs.mobclash.managers;

import io.tjs.mobclash.FileFormat;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LanguageManager {

  /**
   * language.yml's migration steps, see {@link FileFormat#upgrade}. 0 to 1, in 2.2.0: usage and
   * help text moved to plugin.yml, so the server's copy of the old lines is dead text; {@code
   * chest-missing} gained the chest's name and place, which the old line has no placeholders for;
   * and {@code setchest-success} says "Chest", since it also sets gear chests.
   */
  public static final List<Consumer<ConfigurationSection>> FORMAT =
      List.of(
          FileFormat.removeKeys(
              key ->
                  key.endsWith("-usage")
                      || key.startsWith("help-")
                      || key.equals("chest-missing")
                      || key.equals("setchest-success")));

  private final JavaPlugin plugin;
  private FileConfiguration langConfig;

  public LanguageManager(JavaPlugin plugin) {
    this.plugin = plugin;
    reload();
  }

  /** Re-read language.yml from the data folder, writing the bundled copy first if it is missing. */
  public void reload() {
    File langFile = new File(plugin.getDataFolder(), "language.yml");
    if (!langFile.exists()) {
      plugin.saveResource("language.yml", false);
    }
    langConfig = YamlConfiguration.loadConfiguration(langFile);
    if (FileFormat.upgrade(langConfig, "language.yml", FORMAT, plugin.getLogger())) {
      try {
        langConfig.save(langFile);
      } catch (IOException e) {
        plugin.getLogger().log(Level.WARNING, "Could not write the updated " + langFile, e);
      }
    }
    // The data-folder copy is written once, so every key a later release adds is missing from it
    // on an upgraded server. The bundled copy fills those gaps without overriding local edits.
    langConfig.setDefaults(
        YamlConfiguration.loadConfiguration(
            new InputStreamReader(plugin.getResource("language.yml"), StandardCharsets.UTF_8)));
  }

  public String getMessage(String key, Object... replacements) {
    // getString(key, fallback) would skip the bundled defaults; only the one-argument form reads
    // them.
    String message = langConfig.getString(key);
    if (message == null) {
      message = "§cMissing translation: " + key;
    }
    for (int i = 0; i < replacements.length; i++) {
      message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
    }
    return message.replace("&", "§");
  }
}
