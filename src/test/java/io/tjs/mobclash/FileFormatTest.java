package io.tjs.mobclash;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileFormatTest {

  private final List<String> log = new ArrayList<>();
  private final Logger logger = Logger.getLogger("FileFormatTest");
  private final List<Integer> ran = new ArrayList<>();
  private List<Consumer<ConfigurationSection>> steps;

  @BeforeEach
  void setUp() {
    logger.setUseParentHandlers(false);
    for (Handler h : logger.getHandlers()) {
      logger.removeHandler(h);
    }
    logger.addHandler(
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            log.add(record.getLevel() + " " + record.getMessage());
          }

          @Override
          public void flush() {}

          @Override
          public void close() {}
        });
    steps = List.of(file -> ran.add(0), file -> ran.add(1));
  }

  @Test
  void anUnversionedFileRunsEveryStepAndIsStamped() {
    YamlConfiguration file = new YamlConfiguration();

    assertTrue(FileFormat.upgrade(file, "x.yml", steps, logger));
    assertEquals(List.of(0, 1), ran);
    assertEquals(2, file.getInt(FileFormat.KEY));
    assertEquals(List.of("INFO Updated x.yml from format 0 to 2"), log);
  }

  @Test
  void onlyTheStepsAfterTheFilesVersionRun() {
    YamlConfiguration file = new YamlConfiguration();
    file.set(FileFormat.KEY, 1);

    assertTrue(FileFormat.upgrade(file, "x.yml", steps, logger));
    assertEquals(List.of(1), ran);
  }

  @Test
  void aCurrentFileIsLeftAlone() {
    YamlConfiguration file = new YamlConfiguration();
    file.set(FileFormat.KEY, 2);

    assertFalse(FileFormat.upgrade(file, "x.yml", steps, logger));
    assertEquals(List.of(), ran);
    assertEquals(List.of(), log);
  }

  @Test
  void aFileFromANewerReleaseIsLeftAloneWithAWarning() {
    YamlConfiguration file = new YamlConfiguration();
    file.set(FileFormat.KEY, 3);

    assertFalse(FileFormat.upgrade(file, "x.yml", steps, logger));
    assertEquals(List.of(), ran);
    assertEquals(3, file.getInt(FileFormat.KEY));
    assertTrue(log.get(0).startsWith("WARNING x.yml is format 3"));
  }

  @Test
  void theBundledDefaultsDoNotCountAsTheFilesVersion() {
    YamlConfiguration defaults = new YamlConfiguration();
    defaults.set(FileFormat.KEY, 2);
    YamlConfiguration file = new YamlConfiguration();
    file.setDefaults(defaults);

    assertTrue(FileFormat.upgrade(file, "x.yml", steps, logger));
    assertEquals(List.of(0, 1), ran);
  }

  @Test
  void aRemovedKeysCommentsMoveToTheNextKey() throws Exception {
    YamlConfiguration file = new YamlConfiguration();
    file.loadFromString("first: w\n\n# Heading\nold-usage: x\n# Own\nkept: y\nlast-usage: z\n");

    FileFormat.removeKeys(key -> key.endsWith("-usage")).accept(file);

    assertEquals(List.of("first", "kept"), List.copyOf(file.getKeys(false)));
    assertEquals(Arrays.asList(null, "Heading", "Own"), file.getComments("kept"));
  }

  @Test
  void theStampExplainsItselfInTheFile() {
    YamlConfiguration file = new YamlConfiguration();

    FileFormat.upgrade(file, "x.yml", steps, logger);

    assertTrue(file.saveToString().contains("# The layout version of this file."));
  }
}
