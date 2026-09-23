package io.tjs.mobclash.managers;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LanguageManager {

  private final JavaPlugin plugin;
  private FileConfiguration langConfig;

  public LanguageManager(JavaPlugin plugin) {
    this.plugin = plugin;
    loadLanguageFile();
  }

  private void loadLanguageFile() {
    File langFile = new File(plugin.getDataFolder(), "language.yml");
    if (!langFile.exists()) {
      plugin.saveResource("language.yml", false);
    }
    langConfig = YamlConfiguration.loadConfiguration(langFile);
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
