# Changelog

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
