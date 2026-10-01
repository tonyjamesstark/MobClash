# Changelog

## v2.2.0

- The kill board ranks the players in your world, including those with no kills yet, and shows
  the top 10 plus your own line. Before, it ranked every player who had ever killed a MobClash
  mob, wherever they were, so in a test where only one player had kills it showed just them.
- `/killboard` takes a world name: `/killboard world <on|off> <world>` or `/killboard <world>
  <on|off>`, case-insensitive, and also `minecraft:monstermash`. Before, `world` was a literal
  keyword for your own world and nothing else matched. `/killboard on` and `off` set your own,
  and the command tab-completes.
- `/killboard world on` switches the board on for the world: players who arrive later get it,
  and players who leave lose it unless they turned it on themselves. `alloff` clears this too.
- A command block's MobClash replies are also logged to the console, with the block's
  position. Before, a summon refused from a command block left no trace in the server log. An
  error inside any MobClash command is logged with its stack trace and reported to the sender.
- New `/listchests [group]` (also `/mobclash listchests`, permission `mobclash.listchests`,
  default op) lists every chest set with `/setchest`: world, position, and whether a chest is
  still there with how many spawn eggs it holds.
- New `/mobclash version` shows the plugin and server version. Permission `mobclash.version`,
  default everyone.
- Command usage and help text moved from `language.yml` to `plugin.yml`, so they cannot go
  stale on a server whose `language.yml` an older release wrote. A wrong command prints its
  usage lines, coloured, and `/mobclash help` lists each command with its description.
- `config.yml`, `language.yml`, `spawns.yml` and `kills.yml` carry a `format-version`, so a
  release that changes a file's layout updates the server's copy. The first start on 2.2.0
  stamps all four as format 1 and removes the old usage and help lines from `language.yml`. A
  file from a newer release is left alone with a warning. See docs/configuration.md.
- A summon whose wave chest or gear chest is gone names the chest, its group, the world and
  coordinates, and the block found there. It also gives the `/setchest` or `/removechest`
  command that fixes it. Before, it said only "The configured chest no longer exists!", for
  either chest. The first start on 2.2.0 removes that old line from `language.yml`, so the new
  text applies.
- `/setchest` replies "Chest '...' set", not "Spawn egg chest", since it also sets gear chests.
  The first start on 2.2.0 removes the old line from `language.yml`, as for `chest-missing`.
- New `/removechest <group> <chest>` forgets a chest set with `/setchest`. The block is left in
  the world. Permission `mobclash.removechest`, default op.
- `/removespawn <group> <number>` removes the point `/listspawns` shows under that number. It
  works from the console and command blocks too. Without a number, it removes the nearest point
  as before.
- New `/removegroup <group> [confirm]` forgets every spawn point and chest of a group. Without
  `confirm` it only reports what it would remove, so a typo cannot lose a whole arena.
  Permission `mobclash.removegroup`, default op.
- All three also run as `/mobclash removechest`, `/mobclash removespawn` and
  `/mobclash removegroup`.
- New `/killmobs [group]` removes every MobClash mob in a loaded chunk, or one group's. The
  mobs vanish without dying, so there are no drops, XP or kill credit. Permission
  `mobclash.killmobs`, default op. Also runs as `/mobclash killmobs`.
- With `loot-to-inventory` on, a mob whose killer has logged out or died leaves its drops on
  the ground. Before, the drops were cleared and put in that player's inventory. The kill still
  counts.
- `/listspawns` and `/listchests` list a point or chest whose world has been unloaded since the
  server started, and say so. Before, the command failed with an error.

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
