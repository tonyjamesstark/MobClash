# Configuration

All files live in `plugins/MobClash/`.

## config.yml

```yaml
# INFO (verbose), WARNING (quiet), SEVERE (errors only), OFF (silent)
logging-level: INFO

# Put mob drops straight into the killer's inventory. What does not fit still drops.
loot-to-inventory: false

# Most mobs one /summonmobs may spawn. In "all" mode the total is amount x spawn points.
# 0 disables the cap.
max-mobs-per-summon: 500

# Mobs whose death is announced to every player on the server when a player kills one that
# MobClash summoned. [] turns announcements off.
announce-kills: [wither, warden]

# Gear for /summonmobs ... true. See Random equipment in commands.md.
random-equipment:
  # Armor materials, weakest first: leather, copper, golden, chainmail, iron, diamond, netherite
  armor: [leather, copper, golden, iron]
  # Zombie weapons and their relative weights
  weapons:
    copper_sword: 1
    iron_sword: 1
    golden_sword: 1
    copper_shovel: 2
    iron_shovel: 2
    golden_shovel: 2
    copper_axe: 1
    iron_axe: 1
    golden_axe: 1
  # Highest Protection and Sharpness level. 0 turns that enchantment off.
  max-protection: 4
  max-sharpness: 5
```

`announce-kills` takes mob names as in `/summon`, with or without `minecraft:`. The message goes
to every player in every world and to the console. It is `kill-announcement` in
`language.yml`, where `{0}` is the killer and `{1}` the mob's name. A mob's death is
announced only if a player killed it and MobClash summoned it, the same deaths `/kills` counts,
so a wither farm elsewhere on the server stays quiet.

`random-equipment` is used by `/summonmobs ... true`; a gear chest replaces the two lists but
still uses the two levels. `armor` is a ladder: a mob that gets armor takes one tier, and the
first two are the most common, as in vanilla. A `weapons` weight is relative, so `2` is twice as
likely as `1`. Any item works as a weapon.

A `config.yml` from an older version has no `random-equipment` section and uses the defaults
above; so does any key left out of it. An unknown armor material or weapon is skipped with a
console warning at each summon.

`config.yml` is yours. The plugin never writes to it, so edits on a running server are kept.
Run `/mobclash reload` to apply them. If either `config.yml` or `language.yml` has a syntax
error, nothing is reloaded and the console shows where.

## spawns.yml and kills.yml

The plugin writes spawn groups and wave chests to `spawns.yml`, and kill counts to `kills.yml`.
It rewrites them from memory on every save, so edit them only with the server stopped.

## language.yml

Every message can be changed or translated. `{0}`, `{1}` are placeholders, `&` starts a color
code.

```yaml
no-permission: "&cYou don't have permission to use this command."
addspawn-success: "&aAdded spawn point to group '{0}'! Total points: {1}"
```

The file is written once and never overwritten. Any message it lacks, such as one added by a
later release, comes from the plugin's built-in copy. A message a later release changed keeps
its old text until you delete that line, for example `summonmobs-usage`, which gained the
equipment argument.
`/mobclash reload` applies edits without a restart.
