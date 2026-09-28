# Random equipment for /summonmobs

Requests (2026-09-27):
1. Optional boolean parameter on `/summonmobs` to randomize equipment. Armor: leather, copper,
   iron, gold for zombies and skeletons. Weapons: copper, iron, gold shovels and swords for
   zombies. Near-vanilla distribution and amount. Skeletons always get bows. Nothing drops.
   Max enchantment levels in config, protection on armor and sharpness on swords only.
2. Armor and weapon types as config lists, the above as defaults. Skeletons use bows regardless.
3. Option to define a chest in game holding the equipment to draw from.

## Design

Data shape: `RandomEquipment.Pools<T>(List<Map<EquipmentSlot, List<T>>> armor, List<T> weapons)`.
Armor is a ladder of tiers, weakest first; each tier maps a slot to the items it may draw.
Repeats in a list weight the draw. A roll returns `Map<EquipmentSlot, Pick<T>>`, where a Pick is
an item plus the level of its one enchantment (0 for none). `roll` is generic so tests use
plain Materials; at runtime T is ItemStack.

- Config source: `random-equipment.armor` is a list of tier names (`leather` resolves to
  `LEATHER_HELMET` and the rest). `random-equipment.weapons` maps an item to its weight; sword 1,
  shovel 2 per metal is vanilla's 1:2 split.
- Chest source: `/setchest <group> <name>` on a chest of gear, then pass `<name>` instead of
  `true`. The chest is one tier: each armor piece goes to its slot's pool, everything else to
  the weapon pool, each stack weighted by its count. Items keep their own enchantments, names,
  trims and dye; a random enchantment is added only to an item with none.
- The roll, at vanilla's hard-difficulty maximum: 15% chance of armor; tier `nextInt(2)` plus
  three 9.5% bumps, clamped to the ladder; pieces FEET, LEGS, CHEST, HEAD with a 10% chance to
  stop before each after the first. Zombies: 5% chance of a weapon. Armor pieces enchant 50%,
  weapons 25%, level uniform 1..max; max 0 turns it off. Sharpness only where it applies
  (`canEnchantItem`), so shovels stay plain.
- Kits: ZOMBIE, HUSK, ZOMBIE_VILLAGER draw armor and weapons. SKELETON, STRAY, BOGGED, PARCHED
  draw armor and always hold a bow.
- Plain eggs: vanilla's own spawn gear is cleared first. Data eggs: egg gear stays, only empty
  slots fill. All drop chances go to 0 on an equipped mob.
- Parsing: `/summonmobs <group> <wave> <random|all> [amount] [true|false|<gear chest>]`.

## Checklist

Feature playbook:
- [x] Name the data shape (above)
- [x] skip: architect panel, the change is one new class and one command
- [x] skip: delegation, one small change; per CLAUDE.md delegate only divisible work
- [x] Implement `RandomEquipment` (pools, roll, config and chest sources, equip)
- [x] Wire the parameter into `SummonMobsCommand`
- [x] config.yml defaults, language.yml keys, plugin.yml usage
- [x] Unit tests: roll distribution and rules, config parsing, argument parsing
- [x] `mvn -o verify`
- [x] Live check on the `mobclash` instance (backup and restore jar and data)
- [x] Docs: commands.md, configuration.md, CHANGELOG.md

## Results

- Live, 400 mobs per run, config gear: 16% armored, 4.6% of zombies armed, only listed items,
  every skeleton with a bow, no vanilla chainmail or diamond left, Protection 1-4 on about half
  the armor. 400 player kills dropped no gear.
- Gear chest: only chest items, custom name and thorns kept, no Protection added to the
  enchanted helmet, chest contents unchanged.
- Data eggs: the egg's diamond sword kept on all 93 zombies, bows on all 107 skeletons.
- `/mobclashreload` with custom lists and `max-protection: 0`: netherite only, no Protection,
  warnings for the bad entries.
- Found live: a config.yml without the section drew no weapons, because
  getConfigurationSection creates an empty section instead of returning the default. Fixed with
  get; `aConfigFromBeforeTheSettingGetsTheShippedDefaults` and
  `aSettingLeftOutOfTheServersSectionFallsBackToItsDefault` fail without the fix.
- Release check, `mobclash-2.1.0.jar` from `local` over a 2.0.0 config.yml and language.yml:
  17 of 301 zombies armed (5 with axes), every skeleton with a bow, no warnings. 400 player
  kills dropped no gear. Gear chest, data eggs, reload with netherite and `max-protection: 0`,
  a broken YAML refused, and the error messages all behaved as above.
