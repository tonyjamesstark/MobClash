package io.tjs.mobclash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.tjs.mobclash.RandomEquipment.Kit;
import io.tjs.mobclash.RandomEquipment.Pick;
import io.tjs.mobclash.RandomEquipment.Pools;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

class RandomEquipmentTest {

  private static final int ROLLS = 200_000;

  /**
   * Tiers t0..t(n-1), each piece named after its tier and slot, so a pick shows where it came from.
   */
  private static Pools<String> ladder(int tiers, List<String> weapons) {
    List<Map<EquipmentSlot, List<String>>> armor = new ArrayList<>();
    for (int t = 0; t < tiers; t++) {
      Map<EquipmentSlot, List<String>> pieces = new EnumMap<>(EquipmentSlot.class);
      for (EquipmentSlot slot : RandomEquipment.ARMOR_SLOTS) {
        pieces.put(slot, List.of("t" + t + "-" + slot));
      }
      armor.add(pieces);
    }
    return new Pools<>(armor, weapons);
  }

  @Test
  void zombiesGetArmorAndWeaponsAtVanillasHardDifficultyRates() {
    Pools<String> pools = ladder(4, List.of("sword", "shovel", "shovel"));
    Random random = new Random(1);
    int armored = 0;
    int allFour = 0;
    int armed = 0;
    int[] tiers = new int[4];
    int shovels = 0;
    for (int i = 0; i < ROLLS; i++) {
      Map<EquipmentSlot, Pick<String>> picks =
          RandomEquipment.roll(Kit.ZOMBIE, pools, 4, 5, random);
      Pick<String> boots = picks.get(EquipmentSlot.FEET);
      if (boots != null) {
        armored++;
        tiers[boots.item().charAt(1) - '0']++;
        if (picks.keySet().containsAll(RandomEquipment.ARMOR_SLOTS)) {
          allFour++;
        }
      }
      Pick<String> weapon = picks.get(EquipmentSlot.HAND);
      if (weapon != null) {
        armed++;
        if (weapon.item().equals("shovel")) {
          shovels++;
        }
      }
    }
    assertEquals(0.15, armored / (double) ROLLS, 0.005);
    assertEquals(0.05, armed / (double) ROLLS, 0.003);
    assertEquals(0.9 * 0.9 * 0.9, allFour / (double) armored, 0.02);
    assertEquals(2 / 3.0, shovels / (double) armed, 0.03);
    // tier = nextInt(2) + three 9.5% bumps: 37%, 49%, 13%, and 1.3% for the clamped top.
    assertEquals(0.371, tiers[0] / (double) armored, 0.02);
    assertEquals(0.487, tiers[1] / (double) armored, 0.02);
    assertEquals(0.129, tiers[2] / (double) armored, 0.01);
    assertEquals(0.013, tiers[3] / (double) armored, 0.005);
  }

  @Test
  void aPieceIsNeverWornWithoutThePiecesBeforeIt() {
    Pools<String> pools = ladder(4, List.of());
    Random random = new Random(2);
    for (int i = 0; i < ROLLS; i++) {
      Map<EquipmentSlot, Pick<String>> picks =
          RandomEquipment.roll(Kit.ZOMBIE, pools, 4, 5, random);
      String tier = null;
      boolean gap = false;
      for (EquipmentSlot slot : RandomEquipment.ARMOR_SLOTS) {
        Pick<String> pick = picks.get(slot);
        if (pick == null) {
          gap = true;
          continue;
        }
        assertFalse(gap, "a " + slot + " after an empty slot: " + picks);
        String pieceTier = pick.item().substring(0, 2);
        assertTrue(tier == null || tier.equals(pieceTier), "mixed tiers: " + picks);
        tier = pieceTier;
      }
    }
  }

  @Test
  void skeletonsNeverDrawAWeapon() {
    Pools<String> pools = ladder(1, List.of("sword"));
    Random random = new Random(3);
    for (int i = 0; i < ROLLS; i++) {
      assertFalse(
          RandomEquipment.roll(Kit.SKELETON, pools, 4, 5, random).containsKey(EquipmentSlot.HAND));
    }
  }

  @Test
  void aOneTierLadderTakesEveryArmoredMob() {
    Pools<String> pools = ladder(1, List.of());
    Random random = new Random(4);
    int armored = 0;
    for (int i = 0; i < ROLLS; i++) {
      Pick<String> boots =
          RandomEquipment.roll(Kit.ZOMBIE, pools, 4, 5, random).get(EquipmentSlot.FEET);
      if (boots != null) {
        armored++;
        assertEquals("t0-FEET", boots.item());
      }
    }
    assertEquals(0.15, armored / (double) ROLLS, 0.005);
  }

  @Test
  void enchantmentLevelsStayWithinTheConfiguredMaximum() {
    Pools<String> pools = ladder(4, List.of("sword"));
    Random random = new Random(5);
    int[] protection = new int[4];
    int armorPieces = 0;
    for (int i = 0; i < ROLLS; i++) {
      for (Map.Entry<EquipmentSlot, Pick<String>> e :
          RandomEquipment.roll(Kit.ZOMBIE, pools, 3, 2, random).entrySet()) {
        int level = e.getValue().level();
        if (e.getKey() == EquipmentSlot.HAND) {
          assertTrue(level >= 0 && level <= 2, "sharpness " + level);
        } else {
          armorPieces++;
          protection[level]++;
        }
      }
    }
    assertEquals(0.5, protection[0] / (double) armorPieces, 0.02);
    for (int level = 1; level <= 3; level++) {
      assertEquals(0.5 / 3, protection[level] / (double) armorPieces, 0.02);
    }
  }

  @Test
  void aMaximumOfZeroTurnsEnchantingOff() {
    Pools<String> pools = ladder(4, List.of("sword"));
    Random random = new Random(6);
    for (int i = 0; i < ROLLS; i++) {
      for (Pick<String> pick : RandomEquipment.roll(Kit.ZOMBIE, pools, 0, 0, random).values()) {
        assertEquals(0, pick.level());
      }
    }
  }

  @Test
  void emptyPoolsGiveNothing() {
    Pools<String> pools = new Pools<>(List.of(), List.of());
    Random random = new Random(7);
    for (int i = 0; i < 10_000; i++) {
      assertTrue(RandomEquipment.roll(Kit.ZOMBIE, pools, 4, 5, random).isEmpty());
    }
  }

  @Test
  void theShippedDefaultsAreTheRequestedGear() throws Exception {
    YamlConfiguration config = shippedConfig();
    List<String> warnings = new ArrayList<>();

    Pools<Material> pools = RandomEquipment.fromConfig(config, warnings::add);

    assertEquals(List.of(), warnings);
    assertEquals(
        List.of(
            Material.LEATHER_HELMET,
            Material.COPPER_HELMET,
            Material.GOLDEN_HELMET,
            Material.IRON_HELMET),
        pools.armor().stream().map(tier -> tier.get(EquipmentSlot.HEAD).get(0)).toList());
    assertEquals(Material.IRON_BOOTS, pools.armor().get(3).get(EquipmentSlot.FEET).get(0));
    for (Material metal :
        List.of(
            Material.COPPER_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.COPPER_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE)) {
      assertEquals(1, pools.weapons().stream().filter(metal::equals).count(), metal.name());
    }
    for (Material metal :
        List.of(Material.COPPER_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL)) {
      assertEquals(2, pools.weapons().stream().filter(metal::equals).count(), metal.name());
    }
    assertEquals(12, pools.weapons().size());
    assertEquals(4, config.getInt("random-equipment.max-protection"));
    assertEquals(5, config.getInt("random-equipment.max-sharpness"));
  }

  private YamlConfiguration shippedConfig() throws Exception {
    YamlConfiguration config = new YamlConfiguration();
    try (var in = getClass().getResourceAsStream("/config.yml")) {
      config.load(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
    return config;
  }

  /** A server's config.yml over the jar's, as JavaPlugin.getConfig sets it up. */
  private YamlConfiguration serverConfig(String yaml) throws Exception {
    YamlConfiguration config = new YamlConfiguration();
    config.loadFromString(yaml);
    config.setDefaults(shippedConfig());
    return config;
  }

  @Test
  void aConfigFromBeforeTheSettingGetsTheShippedDefaults() throws Exception {
    Pools<Material> pools =
        RandomEquipment.fromConfig(serverConfig("max-mobs-per-summon: 500\n"), warning -> {});

    assertEquals(4, pools.armor().size());
    assertEquals(12, pools.weapons().size());
  }

  @Test
  void aSettingLeftOutOfTheServersSectionFallsBackToItsDefault() throws Exception {
    Pools<Material> pools =
        RandomEquipment.fromConfig(
            serverConfig("random-equipment:\n  armor: [iron]\n"), warning -> {});

    assertEquals(1, pools.armor().size());
    assertEquals(12, pools.weapons().size());
  }

  @Test
  void unknownConfigEntriesAreSkippedWithAWarning() throws InvalidConfigurationException {
    YamlConfiguration config = new YamlConfiguration();
    config.loadFromString(
        """
        random-equipment:
          armor: [leather, bogus, turtle]
          weapons:
            iron_sword: 1
            iron_shovel: 2
            nonsense: 1
            stone_sword: 0
        """);
    List<String> warnings = new ArrayList<>();

    Pools<Material> pools = RandomEquipment.fromConfig(config, warnings::add);

    assertEquals(2, pools.armor().size());
    assertEquals(4, pools.armor().get(0).size());
    assertEquals(Map.of(EquipmentSlot.HEAD, List.of(Material.TURTLE_HELMET)), pools.armor().get(1));
    assertEquals(
        List.of(Material.IRON_SWORD, Material.IRON_SHOVEL, Material.IRON_SHOVEL), pools.weapons());
    assertEquals(3, warnings.size(), warnings.toString());
  }
}
