package io.tjs.mobclash;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The check /mobclash reload runs before replacing anything. */
class RequireParsesTest {

  @TempDir Path dir;

  @Test
  void validYamlPasses() throws IOException {
    Path file = Files.writeString(dir.resolve("config.yml"), "loot-to-inventory: true\n");
    assertDoesNotThrow(() -> MobClashPlugin.requireParses(file.toFile()));
  }

  @Test
  void brokenYamlIsRejectedByName() throws IOException {
    // The smoke test's own mistake: a line appended to a file with no trailing newline.
    Path file =
        Files.writeString(
            dir.resolve("language.yml"), "resetall-success: \"&aDone\"reload-success: \"x\"\n");
    InvalidConfigurationException e =
        assertThrows(
            InvalidConfigurationException.class, () -> MobClashPlugin.requireParses(file.toFile()));
    assertEquals("language.yml", e.getMessage());
  }

  @Test
  void aMissingFilePasses() {
    assertDoesNotThrow(() -> MobClashPlugin.requireParses(dir.resolve("config.yml").toFile()));
  }
}
