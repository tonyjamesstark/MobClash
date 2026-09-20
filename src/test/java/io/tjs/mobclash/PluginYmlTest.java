package io.tjs.mobclash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * plugin.yml is not compiled, so nothing else catches a mistake in it: a malformed descriptor stops
 * the plugin loading at all, and a permission node that exists only in Java silently denies every
 * non-op. Read as plain YAML rather than through YamlConfiguration, because Bukkit's own
 * PluginDescriptionFile does the same and treats a dotted key as one literal key, where
 * ConfigurationSection would split it into a path. The copy read is the filtered one from
 * target/classes, so resource filtering is exercised too.
 */
class PluginYmlTest {

  private static final Pattern PERMISSION_NODE = Pattern.compile("\"(mobclash\\.[a-z.]+)\"");

  @SuppressWarnings("unchecked")
  private Map<String, Object> descriptor() throws IOException {
    try (InputStream in = getClass().getResourceAsStream("/plugin.yml")) {
      assertNotNull(in, "plugin.yml is missing from the built classpath");
      Object parsed = new Yaml().load(in);
      assertTrue(parsed instanceof Map, "plugin.yml did not parse to a mapping");
      return (Map<String, Object>) parsed;
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> section(String name) throws IOException {
    Object value = descriptor().get(name);
    assertNotNull(value, "plugin.yml has no '" + name + "' section");
    return (Map<String, Object>) value;
  }

  /** Every permission node named in Java, found by scanning the sources. */
  private Set<String> nodesUsedInCode() throws IOException {
    Set<String> nodes = new TreeSet<>();
    try (Stream<Path> sources = Files.walk(Path.of("src/main/java"))) {
      for (Path source : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
        Matcher matcher = PERMISSION_NODE.matcher(Files.readString(source));
        while (matcher.find()) {
          nodes.add(matcher.group(1));
        }
      }
    }
    assertFalse(nodes.isEmpty(), "found no permission nodes to check; the scan pattern is stale");
    return nodes;
  }

  @Test
  void theDescriptorParsesAndNamesThePlugin() throws IOException {
    Map<String, Object> yml = descriptor();
    assertEquals("MobClash", yml.get("name"));
    assertEquals("io.tjs.mobclash.MobClashPlugin", yml.get("main"));
  }

  @Test
  void theVersionPlaceholderIsSubstitutedByResourceFiltering() throws IOException {
    String version = String.valueOf(descriptor().get("version"));
    assertFalse(version.contains("${"), "version was not filtered: " + version);
  }

  @Test
  void everyPermissionNodeUsedInCodeIsDeclared() throws IOException {
    Set<String> declared = section("permissions").keySet();
    List<String> undeclared =
        nodesUsedInCode().stream().filter(n -> !declared.contains(n)).toList();
    assertTrue(undeclared.isEmpty(), "used in code but not declared in plugin.yml: " + undeclared);
  }

  @Test
  @SuppressWarnings("unchecked")
  void theWildcardGrantsEveryNodeUsedInCode() throws IOException {
    Map<String, Object> wildcard = (Map<String, Object>) section("permissions").get("mobclash.*");
    assertNotNull(wildcard, "plugin.yml declares no mobclash.* permission");
    Set<String> children = ((Map<String, Object>) wildcard.get("children")).keySet();
    List<String> ungranted = nodesUsedInCode().stream().filter(n -> !children.contains(n)).toList();
    assertTrue(ungranted.isEmpty(), "nodes not granted by mobclash.*: " + ungranted);
  }

  @Test
  @SuppressWarnings("unchecked")
  void theDeprecatedNamespaceStillGrantsTheNewNodes() throws IOException {
    Map<String, Object> permissions = section("permissions");
    for (String node : nodesUsedInCode()) {
      String legacy = node.replace("mobclash.", "mobspawner.");
      Map<String, Object> alias = (Map<String, Object>) permissions.get(legacy);
      assertNotNull(alias, "no backward-compatible alias declared for " + node);
      Set<String> children = ((Map<String, Object>) alias.get("children")).keySet();
      assertTrue(children.contains(node), legacy + " does not grant " + node);
    }
  }

  @Test
  void everyDeclaredCommandIsRegisteredByThePlugin() throws IOException {
    // getCommand returns null for a command missing from plugin.yml, and the reverse -- a command
    // declared here with no executor -- silently prints the usage line instead of running.
    Set<String> declared = section("commands").keySet();
    String source = Files.readString(Path.of("src/main/java/io/tjs/mobclash/MobClashPlugin.java"));
    List<String> unregistered =
        declared.stream().filter(name -> !source.contains("\"" + name + "\"")).toList();
    assertTrue(
        unregistered.isEmpty(), "declared in plugin.yml but never registered: " + unregistered);
  }
}
