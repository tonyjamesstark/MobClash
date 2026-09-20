# MobClash v1.2.0

Paste this into the GitHub release body. Attach `target/mobclash-1.2.0.jar`.

---

Built and tested against Paper 1.20.4 on Java 17.

## Before you upgrade

Three things change on disk. All three migrate themselves, but they are worth knowing about.

**Permissions moved to `mobclash.*`.** The plugin was renamed a while ago; the permission nodes
were not. `mobspawner.addspawn` and friends are still declared, each as a parent of its
`mobclash.*` replacement, so an existing permissions config keeps working with no edit. They
default to `false` and exist only for this migration -- grant `mobclash.*` from here on.

**Spawn points and kill counts moved out of `config.yml`** into `spawns.yml` and `kills.yml`.
The first start after upgrading moves them across and logs that it did. Nothing else is needed.

Why it matters: the plugin used to rewrite `config.yml` from memory on every `/addspawn`. If you
edited that file while the server was running, your edit was gone at the next command, silently.
`config.yml` is now yours alone.

**New setting: `max-mobs-per-summon`**, default 500. `/summonmobs <group> <wave> all <amount>`
spawns `amount x spawn points` entities in a single tick, which was previously unbounded. Set it
to `0` if you want the old behaviour.

## Fixes

- A missing or unloaded world no longer erases your spawn configuration. The save now builds the
  whole file before clearing anything, so a location it cannot write costs that location rather
  than the file.
- **Command blocks work.** The bypass existed but never ran: `plugin.yml` declared a permission
  per command, so Bukkit rejected the command block before the plugin's own check saw it. The
  console is now covered explicitly rather than by happening to be an operator.
- `/addspawn` and `/removespawn` from a command block use the command block's own position.
  `/setchest` still needs a player, since it picks the chest you are looking at.
- Mooshroom and snow golem spawn eggs in a wave chest were being ignored. Eggs now resolve
  through their namespaced key rather than by matching enum names, which those two do not.
- `/kills top -1` no longer throws for every player on the server.
- Kill data that is corrupt or hand-edited is discarded entry by entry with a warning, instead of
  stopping the plugin from loading.
- Loot sent to a full inventory could duplicate on a server whose `addItem` copies rather than
  decrements in place. It now uses what the API hands back.
- Mobs that a protection plugin refuses to spawn are no longer reported as spawned.
- Group and wave names containing a `.` are rejected. They used to nest as a config path and
  quietly vanish on the next reload.
- `/showspawns` draws particles. It used to spawn real `AreaEffectCloud` entities, which `@e`
  selectors could see and which could be written into the chunk.
- `/version MobClash` reports the real version, rather than always `1.0.0`.

## For anyone building from source

The Maven coordinates changed from `com.example` to `io.tjs`, and the shade plugin is gone --
there was nothing to shade. Both are source-only changes with no effect on the jar you install.

## Tests

72 unit tests and 8 integration tests, all passing. The previous release shipped with 8 of them
erroring; `TEST-SUMMARY.md` claimed a clean run that had never happened. New coverage includes
the storage migration, the `plugin.yml` descriptor that nothing else checks, and a persistence
round trip that goes through real YAML on disk.

## Install

1. Stop the server.
2. Replace the old jar in `plugins/` with `mobclash-1.2.0.jar`.
3. Start the server and check the log for the line about moving data into `spawns.yml`.
