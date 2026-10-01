package io.tjs.mobclash;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;

/**
 * The format version every MobClash YAML file carries, so a release that changes a file's layout
 * can update the copy already on a server instead of leaving it stale. A file without the key
 * predates versioning and counts as 0.
 */
public final class FileFormat {

  public static final String KEY = "format-version";

  private FileFormat() {}

  /**
   * Bring a file up to the current format. {@code steps.get(v)} moves version {@code v} to {@code v
   * + 1}, so the current version is {@code steps.size()}. A file from a newer MobClash is left as
   * it is, with a warning, since an older release cannot know what changed.
   *
   * <p>The version is read ignoring defaults: config.yml and language.yml carry the jar's copy as
   * defaults, which would report the current version for a file that has none.
   *
   * @return whether the file changed and needs saving
   */
  public static boolean upgrade(
      ConfigurationSection file,
      String name,
      List<Consumer<ConfigurationSection>> steps,
      Logger logger) {
    int current = steps.size();
    int version = file.contains(KEY, true) ? file.getInt(KEY) : 0;
    if (version > current) {
      logger.warning(
          name
              + " is format "
              + version
              + ", newer than this MobClash knows ("
              + current
              + "). Leaving it as it is.");
      return false;
    }
    if (version == current) {
      return false;
    }
    for (int v = version; v < current; v++) {
      steps.get(v).accept(file);
    }
    stamp(file, current);
    logger.info("Updated " + name + " from format " + version + " to " + current);
    return true;
  }

  /** Set the file's format version, with a comment saying what it is when the key is new. */
  public static void stamp(ConfigurationSection file, int version) {
    boolean added = !file.contains(KEY, true);
    file.set(KEY, version);
    if (added) {
      file.setComments(
          KEY,
          List.of(
              "The layout version of this file. MobClash updates the file when a release changes"
                  + " it; do not edit."));
    }
  }

  /**
   * A step that deletes the top-level keys matching {@code obsolete}. A deleted key's comments,
   * often a section heading, move to the next key that stays.
   */
  public static Consumer<ConfigurationSection> removeKeys(Predicate<String> obsolete) {
    return file -> {
      List<String> carried = new ArrayList<>();
      for (String key : file.getKeys(false)) {
        if (obsolete.test(key)) {
          carried.addAll(file.getComments(key));
          file.set(key, null);
        } else if (!carried.isEmpty()) {
          carried.addAll(file.getComments(key));
          // Not List.copyOf: Bukkit keeps a blank line in a comment block as a null entry.
          file.setComments(key, new ArrayList<>(carried));
          carried.clear();
        }
      }
    };
  }

  /** The step for a file whose first versioned format is its unversioned one: only the stamp. */
  public static final Consumer<ConfigurationSection> STAMP = file -> {};
}
