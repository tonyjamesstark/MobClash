# Changelog

## v2.1.0

- `/summonmobs` takes an optional last argument for random equipment: `true` for the lists in
  the new `random-equipment` section of `config.yml`, or the name of a chest set with
  `/setchest` to draw from that chest. Near-vanilla odds, Protection and Sharpness up to the
  configured levels, skeletons always hold a bow, and none of it drops. An existing
  `language.yml` keeps its old `summonmobs-usage` line until you delete that key.
- `/mobclash <command>` runs any MobClash command, and `/mobclash help` lists the ones you
  may use, with tab completion. The standalone commands such as `/summonmobs` still work.
- A player's kill of a summoned wither or warden is announced to the whole server. The
  `announce-kills` list in `config.yml` sets which mobs; `[]` turns it off.
- The deprecated `mobspawner.*` permission nodes are gone. Grant the matching `mobclash.*`
  nodes instead; see [permissions.md](docs/permissions.md).
- `/mobclash reload` re-reads `config.yml` and `language.yml` without restarting the server.
  Needs `mobclash.reload`, default op. A file with a syntax error stops the reload and is named.
- Spawn eggs with `entity_data` (attributes, health, `DeathLootTable`, `PersistenceRequired`)
  now summon the mob they describe. Before, only the mob type was used. Such mobs get no random
  spawn gear, and gear the egg gives never drops.
- Eggs whose `entity_data` is not a mob, has riders or fixes a `UUID` are skipped with a warning
  to the console and the summoner. So is an egg with no matching mob, which only logged before.
- A death cancelled by a plugin listening before MobClash's `HIGHEST` handler (a revive) no
  longer counts as a kill or hands its drops to the killer under `loot-to-inventory`.

## v2.0.0

- Requires Paper 1.21.11 and Java 21. Mobs spawned under 1.2.0 keep counting towards kills.
- `/summonmobs ... random N` puts each mob at its own random spawn point, instead of all N at
  one.
- `/killboard`: the kill leaderboard in the sidebar, per player or for a whole world.
- Messages missing from an older `language.yml` fall back to the built-in text instead of
  showing "Missing translation".
- Each command is registered with its permission node, so players who lack it no longer see it
  in tab completion. Typing it gives "Unknown command" instead of the `no-permission` message.

## v1.2.0

Behaviour changes that need a word before you upgrade:

- Permissions moved from `mobspawner.*` to `mobclash.*`. The old nodes still work -- each is
  declared as a parent of its replacement -- so an existing permissions config needs no edit.
  New grants should use `mobclash.*`.
- Spawn points and kill counts moved out of `config.yml` into `spawns.yml` and `kills.yml`. The
  first start after upgrading moves them across automatically and logs that it did.
- `max-mobs-per-summon` caps what a single `/summonmobs` may spawn, default 500.

Fixes:

- A missing or unloaded world no longer erases the spawn configuration.
- The command-block permission bypass works. It never ran before: `plugin.yml` declared a
  permission per command, so Bukkit's own check rejected the sender first.
- Mooshroom and snow golem spawn eggs are no longer ignored.
- `/kills top` no longer throws when given a negative count.
- Corrupt or hand-edited kill data is discarded per entry instead of stopping the plugin loading.
- Loot sent to a full inventory is no longer at risk of duplicating.
- Mobs a protection plugin refuses to spawn are no longer counted as spawned.
- Group and wave names containing a `.` are rejected rather than silently failing to reload.
- `/showspawns` draws particles instead of spawning real entities that `@e` selectors could see.
- `/version MobClash` reports the real version.

## v1.1.0
- Loot-to-inventory handling
- Player kill tracking and the `/kills` command

## v1.0.0 (Initial Release)
- Multiple spawn groups with configurable points
- Per-group chest system
- Random and all-location spawn modes
- Visual spawn markers
- Command block support
- Configurable logging
- Full test coverage
- Translatable messages
