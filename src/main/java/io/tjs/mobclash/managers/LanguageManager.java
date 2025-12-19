package io.tjs.mobclash.managers;

import java.io.File;
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
  }

  public String getMessage(String key, Object... replacements) {
    String message = langConfig.getString(key, "§cMissing translation: " + key);
    for (int i = 0; i < replacements.length; i++) {
      message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
    }
    return message.replace("&", "§");
  }
}
