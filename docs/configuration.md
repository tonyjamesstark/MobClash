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

`loot-to-inventory` moves a mob's drops into the inventory of the player who killed it. If that
player has logged out or died by the time the mob dies, the drops stay on the ground and the
kill still counts. A mob that dies without a player killing it drops its loot as usual. Only
mobs MobClash summoned are affected.

A wither MobClash summoned never drops its nether star, with `loot-to-inventory` on or off and
whoever or whatever kills it. The game drops the star outside the wither's loot table, so a
`DeathLootTable` on the egg cannot remove it. A wither summoned any other way keeps its star.

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

`config.yml` is yours. The plugin writes to it only when a release changes its layout (see
Format version below), so edits on a running server are kept.
Run `/mobclash reload` to apply them. If either `config.yml` or `language.yml` has a syntax
error, nothing is reloaded and the console shows where.

## spawns.yml and kills.yml

The plugin writes spawn groups and chests to `spawns.yml`, and kill counts to `kills.yml`.
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
its old text until you delete that line.
`/mobclash reload` applies edits without a restart.

Command usage and help text are not in this file. They come from the plugin's `plugin.yml`,
so `/mobclash help` and the usage shown after a wrong command always match the release.

## Format version

`config.yml`, `language.yml`, `spawns.yml` and `kills.yml` each carry a `format-version` line.
When a release changes a file's layout, MobClash updates the server's copy at startup (and on
`/mobclash reload` for `config.yml` and `language.yml`), keeping your values, and logs
`Updated <file> from format N to M`. Do not edit the number. A file without it counts as
format 0, which is every file written before 2.2.0. A file with a higher number than the
running MobClash knows, after a downgrade, is left as it is with a console warning.

| Format | Release | Change |
|---|---|---|
| 1 | 2.2.0 | `format-version` added. `language.yml` loses its `*-usage`, `help-*` and `help-header` lines, now in `plugin.yml`. It also loses its `chest-missing` line, so the new text that names the chest and its position applies, and its `setchest-success` line, which now says "Chest" rather than "Spawn egg chest". |
| 2 | 2.2.1 | `language.yml` only. It loses its `must-look-chest` line, so the new text that names barrels and shulker boxes applies. `config.yml`, `spawns.yml` and `kills.yml` stay at format 1. |
