# MobClash v2.1.0

Paste this into the GitHub release body. Attach `target/mobclash-2.1.0.jar`.

---

Built and tested against Paper 1.21.11 on Java 21.

## Before you upgrade

**The old `mobspawner.*` permission nodes are gone.** They were kept as aliases after the rename
in 1.2.0. If your permissions plugin still grants any of them, grant the matching `mobclash.*`
node instead, or those players lose the command.

Your data and configuration carry over. `config.yml` is never rewritten, so the new settings
below use their defaults until you add them. `language.yml` is not rewritten either: new
messages come from the built-in copy, but the `summonmobs-usage` line keeps its old text until
you delete it.

## New

**Random equipment for summoned mobs.** `/summonmobs` takes an optional last argument:

- `true` draws armor and weapons from the new `random-equipment` section of `config.yml`. The
  defaults are leather, copper, golden and iron armor, and copper, iron and golden swords,
  shovels and axes.
- The name of a chest set with `/setchest` draws from that chest instead. Items keep their
  names, trims, dye and enchantments.
- `false`, the default, leaves vanilla's spawn gear alone.

The odds are vanilla's on hard difficulty: 15% of mobs get armor and 5% of zombies a weapon.
Protection and Sharpness go up to `max-protection` and `max-sharpness`. Skeletons, strays,
bogged and parched always hold a bow. Nothing a summoned mob wears or holds ever drops.

**`/mobclash` holds every command.** `/mobclash help` lists the commands you may use, and tab
completion offers them. `/mobclash summonmobs ...`, `/mobclash kills top` and the rest run the
same as the standalone commands, which still work, so existing command blocks keep running.

**`/mobclash reload` applies config edits without a restart.** It re-reads `config.yml` and
`language.yml`. If either file has a syntax error, nothing is reloaded and the console shows
where. It needs `mobclash.reload`, which defaults to op.

**Wither and warden kills are announced to the whole server.** When a player kills a wither or
warden that MobClash summoned, every player in every world sees who did it. `announce-kills` in
`config.yml` sets which mobs are announced, and `[]` turns it off. The message is
`kill-announcement` in `language.yml`.

## Changes

- **Spawn eggs keep their entity data.** An egg from `/give` with custom attributes, health, a
  loot table or persistence now summons that mob. Before, only the mob type was used.
- **Bad eggs are skipped with a warning.** An egg whose entity data is not a mob, has riders or
  fixes a UUID is skipped, and both the console and whoever ran the summon are told. So is an
  egg with no matching mob, which only logged before.

## Fixes

- A death that another plugin cancels to revive the mob no longer counts as a kill, and no
  longer hands its drops to the killer under `loot-to-inventory`.

## Tests

136 unit tests and 8 integration tests, all passing. Checked live on a Purpur 1.21.11 server
with an op and a non-op player. The live check covered:

- an upgrade over a 2.0.0 data folder, `config.yml` and `language.yml`
- random equipment from `config.yml` and from a gear chest over 400 mobs, and 400 kills with no
  gear dropped
- eggs with entity data, and an egg with riders skipped
- `/mobclash reload` with an edited config and with a broken one
- `/mobclash help` and tab completion for an op and a non-op player
- wither and warden kills announced to a player in another world, and other kills not announced

## Install

1. Stop the server.
2. Replace the old jar in `plugins/` with `mobclash-2.1.0.jar`.
3. If you granted `mobspawner.*` nodes, switch them to `mobclash.*`.
4. Start the server.
