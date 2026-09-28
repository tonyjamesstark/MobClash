package io.tjs.mobclash;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Spawn gear for zombies and skeletons, drawn the way vanilla draws it on hard difficulty at full
 * regional difficulty, but from the config's lists or a chest of gear instead of vanilla's table.
 */
public final class RandomEquipment {

  public enum Kit {
    ZOMBIE,
    SKELETON
  }

  public static final Map<EntityType, Kit> KITS =
      Map.of(
          EntityType.ZOMBIE, Kit.ZOMBIE,
          EntityType.HUSK, Kit.ZOMBIE,
          EntityType.ZOMBIE_VILLAGER, Kit.ZOMBIE,
          EntityType.SKELETON, Kit.SKELETON,
          EntityType.STRAY, Kit.SKELETON,
          EntityType.BOGGED, Kit.SKELETON,
          EntityType.PARCHED, Kit.SKELETON);

  /**
   * What a mob may draw. {@code armor} is a ladder of tiers, weakest first, each mapping a slot to
   * its items; {@code weapons} are zombie weapons. A repeated item is proportionally more likely.
   */
  public record Pools<T>(List<Map<EquipmentSlot, List<T>>> armor, List<T> weapons) {
    public <R> Pools<R> map(Function<? super T, ? extends R> f) {
      List<Map<EquipmentSlot, List<R>>> tiers = new ArrayList<>();
      for (Map<EquipmentSlot, List<T>> tier : armor) {
        Map<EquipmentSlot, List<R>> mapped = new EnumMap<>(EquipmentSlot.class);
        tier.forEach((slot, items) -> mapped.put(slot, items.stream().<R>map(f).toList()));
        tiers.add(mapped);
      }
      return new Pools<>(tiers, weapons.stream().<R>map(f).toList());
    }
  }

  /** An item drawn for a slot, and the level of the one enchantment it may get; 0 for none. */
  public record Pick<T>(T item, int level) {}

  static final float ARMOR_CHANCE = 0.15f;
  static final float TIER_BUMP = 0.095f;
  static final float STOP_BEFORE_PIECE = 0.1f;
  static final float WEAPON_CHANCE = 0.05f;
  static final float ARMOR_ENCHANT_CHANCE = 0.5f;
  static final float WEAPON_ENCHANT_CHANCE = 0.25f;

  /** Vanilla's fill order: a mob with one piece has boots, with two boots and leggings. */
  static final List<EquipmentSlot> ARMOR_SLOTS =
      List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);

  private static final Map<EquipmentSlot, String> PIECE_SUFFIX =
      Map.of(
          EquipmentSlot.FEET, "_boots",
          EquipmentSlot.LEGS, "_leggings",
          EquipmentSlot.CHEST, "_chestplate",
          EquipmentSlot.HEAD, "_helmet");

  private RandomEquipment() {}

  /** Draw one mob's gear. Skeletons draw no weapon here; {@link #equip} hands them a bow. */
  public static <T> Map<EquipmentSlot, Pick<T>> roll(
      Kit kit, Pools<T> pools, int maxProtection, int maxSharpness, Random random) {
    Map<EquipmentSlot, Pick<T>> picks = new EnumMap<>(EquipmentSlot.class);
    if (!pools.armor().isEmpty() && random.nextFloat() < ARMOR_CHANCE) {
      int tier = random.nextInt(2);
      for (int i = 0; i < 3; i++) {
        if (random.nextFloat() < TIER_BUMP) {
          tier++;
        }
      }
      Map<EquipmentSlot, List<T>> pieces =
          pools.armor().get(Math.min(tier, pools.armor().size() - 1));
      for (EquipmentSlot slot : ARMOR_SLOTS) {
        if (slot != EquipmentSlot.FEET && random.nextFloat() < STOP_BEFORE_PIECE) {
          break;
        }
        List<T> items = pieces.get(slot);
        if (items != null && !items.isEmpty()) {
          picks.put(
              slot,
              new Pick<>(draw(items, random), level(ARMOR_ENCHANT_CHANCE, maxProtection, random)));
        }
      }
    }
    if (kit == Kit.ZOMBIE && !pools.weapons().isEmpty() && random.nextFloat() < WEAPON_CHANCE) {
      picks.put(
          EquipmentSlot.HAND,
          new Pick<>(
              draw(pools.weapons(), random), level(WEAPON_ENCHANT_CHANCE, maxSharpness, random)));
    }
    return picks;
  }

  private static <T> T draw(List<T> items, Random random) {
    return items.get(random.nextInt(items.size()));
  }

  private static int level(float chance, int max, Random random) {
    return max > 0 && random.nextFloat() < chance ? 1 + random.nextInt(max) : 0;
  }

  /**
   * The pools under {@code random-equipment}: {@code armor} lists tier names weakest first ({@code
   * iron} for IRON_HELMET and the rest), {@code weapons} maps an item to its weight.
   *
   * <p>Lookups use get, not getConfigurationSection: when config.yml lacks a key, the latter
   * creates an empty section in its place instead of returning the jar's default, and a config.yml
   * from before this setting lacks all of them.
   */
  public static Pools<Material> fromConfig(ConfigurationSection config, Consumer<String> warn) {
    if (!(config.get("random-equipment") instanceof ConfigurationSection section)) {
      warn.accept("config.yml has no random-equipment section");
      return new Pools<>(List.of(), List.of());
    }
    List<Map<EquipmentSlot, List<Material>>> tiers = new ArrayList<>();
    for (String tier : section.getStringList("armor")) {
      Map<EquipmentSlot, List<Material>> pieces = new EnumMap<>(EquipmentSlot.class);
      PIECE_SUFFIX.forEach(
          (slot, suffix) -> {
            Material piece = Material.matchMaterial(tier + suffix);
            if (piece != null) {
              pieces.put(slot, List.of(piece));
            }
          });
      if (pieces.isEmpty()) {
        warn.accept("random-equipment.armor: no armor is made of '" + tier + "'");
      } else {
        tiers.add(pieces);
      }
    }
    List<Material> weapons = new ArrayList<>();
    ConfigurationSection weights =
        section.get("weapons") instanceof ConfigurationSection s ? s : null;
    for (String key : weights == null ? List.<String>of() : weights.getKeys(false)) {
      Material weapon = Material.matchMaterial(key);
      int weight = weights.getInt(key);
      if (weapon == null || weight < 1) {
        warn.accept("random-equipment.weapons: '" + key + "' is not an item with a weight of 1+");
        continue;
      }
      for (int i = 0; i < weight; i++) {
        weapons.add(weapon);
      }
    }
    return new Pools<>(tiers, weapons);
  }

  /**
   * The pools a chest of gear gives: one tier holding every armor piece in its slot, and every
   * other item as a zombie weapon, each stack weighted by its count.
   */
  public static Pools<ItemStack> fromChest(Inventory inventory) {
    Map<EquipmentSlot, List<ItemStack>> armor = new EnumMap<>(EquipmentSlot.class);
    List<ItemStack> weapons = new ArrayList<>();
    for (ItemStack item : inventory.getContents()) {
      if (item == null || item.isEmpty()) {
        continue;
      }
      EquipmentSlot slot = item.getType().getEquipmentSlot();
      List<ItemStack> pool =
          ARMOR_SLOTS.contains(slot)
              ? armor.computeIfAbsent(slot, s -> new ArrayList<>())
              : weapons;
      for (int i = 0; i < item.getAmount(); i++) {
        pool.add(item);
      }
    }
    return new Pools<>(armor.isEmpty() ? List.of() : List.of(armor), weapons);
  }

  /**
   * Dress a freshly spawned mob. With {@code keepExisting} (a mob from egg data) only empty slots
   * fill, so the egg's own gear stays; otherwise vanilla's spawn gear is cleared first. Skeletons
   * end up holding a bow. None of the mob's gear drops.
   */
  public static void equip(
      Mob mob, Kit kit, Map<EquipmentSlot, Pick<ItemStack>> picks, boolean keepExisting) {
    EntityEquipment equipment = mob.getEquipment();
    if (!keepExisting) {
      for (EquipmentSlot slot : ARMOR_SLOTS) {
        equipment.setItem(slot, null);
      }
      equipment.setItem(EquipmentSlot.HAND, null);
    }
    picks.forEach(
        (slot, pick) -> {
          if (keepExisting && !isEmpty(equipment.getItem(slot))) {
            return;
          }
          ItemStack item = pick.item().asOne();
          Enchantment enchantment =
              slot == EquipmentSlot.HAND ? Enchantment.SHARPNESS : Enchantment.PROTECTION;
          if (pick.level() > 0
              && item.getEnchantments().isEmpty()
              && enchantment.canEnchantItem(item)) {
            item.addUnsafeEnchantment(enchantment, pick.level());
          }
          equipment.setItem(slot, item);
        });
    if (kit == Kit.SKELETON && isEmpty(equipment.getItem(EquipmentSlot.HAND))) {
      equipment.setItem(EquipmentSlot.HAND, ItemStack.of(Material.BOW));
    }
    for (EquipmentSlot slot : EquipmentSlot.values()) {
      equipment.setDropChance(slot, 0f);
    }
  }

  private static boolean isEmpty(ItemStack item) {
    return item == null || item.isEmpty();
  }
}
