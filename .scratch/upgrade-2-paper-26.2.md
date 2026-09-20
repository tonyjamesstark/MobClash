# Phase 2: MobClash on Paper 26.2

From the finished state of `.scratch/upgrade-1-paper-1.21.11.md`. Do not start this until
phase 1 has shipped and run on a real server for a while.

## The version numbering, since it reads like a typo

Mojang dropped `1.x` in 2026. A version is now the year plus the release number within that
year: 26.1 is the first game drop of 2026, 26.2 the second, and a third digit is a hotfix.
So 26.2 is two drops after 26.1, which itself follows 1.21.11. It is newer, not older.

## What makes this different from phase 1

Phase 1 was a toolchain move with a handful of renames. This one has a real code project inside
it: **Adventure 5**, and the ~50 `sendMessage(String)` call sites that go through
`LanguageManager`. Budget for that, not for the dependency bump.

## Prerequisites

- [ ] **Java 25 installed.** Paper 26.1 and newer require it. Again, yours to run.
- [ ] Verify google-java-format runs on 25, or bump it. This project has hit that wall before.
- [ ] A test server on Paper 26.2 with a **copy** of a production world, because 26.1 moved
      dimensions into `world/dimensions/` and relocated `paper-world.yml`. That migration is
      the server's, not ours, but it is one-way — do not point it at anything you care about.
- [ ] Branch `upgrade/paper-26.2` off the phase 1 release tag.

## Open questions to settle before writing code

These I could not confirm well enough to write down as fact. Settle each by looking, not by
reasoning from the last version:

- [ ] **The Maven coordinate.** Paper 26.x appears to use a build-stamped version string such
      as `26.2.build.124-stable` rather than the familiar `26.2-R0.1-SNAPSHOT`. Check what the
      Paper repo actually serves before editing the pom. If it is build-stamped, decide whether
      to pin a build or track stable, and write the reason in the pom.
- [ ] **`plugin.yml` vs `paper-plugin.yml`.** Both are supported. `api-version` takes
      `major.minor`, so `26.2`, never the build string. Staying on `plugin.yml` is the lazy
      and correct default — we use nothing `paper-plugin.yml` offers. Confirm, then move on.
- [ ] **What Adventure 5 actually removed.** The whole shape of the message work below depends
      on whether `CommandSender.sendMessage(String)` survives. Compile first, then plan.

## Checklist

### Fix the message seam first, on the current version
Do this **before** the bump, as its own commit, so the Adventure change is a type change
rather than a behaviour change.

- [ ] `LanguageManager.java:39` ends with `message.replace("&", "§")`. That replaces *every*
      ampersand, not just colour codes: a message reading `Fish & Chips` renders as
      `Fish § Chips`, and a translator cannot write a literal `&` at all. Replace with
      `ChatColor.translateAlternateColorCodes('&', message)`, which only rewrites `&` followed
      by a colour character.
- [ ] Add a test for a message containing a literal `&`. It fails today.

### The Adventure 5 migration
- [ ] Decide the boundary. Recommendation: `LanguageManager.getMessage` returns a `Component`,
      built with `LegacyComponentSerializer.legacyAmpersand()` so `language.yml` keeps its
      familiar `&a` codes and no operator has to rewrite their translations. MiniMessage is
      the more capable option and the wrong one here — it would break every existing
      `language.yml` on upgrade, for a plugin whose messages are one line each.
- [ ] Change `getMessage` to return `Component`, then follow the compiler through all ~50 call
      sites. They are mechanical: `sender.sendMessage(Component)` is already the preferred
      overload.
- [ ] The command tests stub `getMessage` to echo its key back as a `String`. They need to echo
      a `Component` instead. `Component.text(key)` keeps every existing assertion meaningful —
      keep asserting on the key, not on rendered text.
- [ ] `LanguageManagerTest` needs the same treatment.

### Server-side changes to check, not to fix
- [ ] World storage restructuring. We store **world names** in `spawns.yml` and resolve them
      with `Bukkit.getWorld(name)`. Names should be unaffected by the folder move, but verify
      with a real migrated world before believing it: a spawn group pointing at a nether or end
      dimension is exactly the case that would break, and `SpawnManager` would log
      `world '...' is not loaded` and quietly drop the points.
- [ ] Per-world clocks, beds losing block-entity status, cube mob restructuring: none of these
      touch anything we call. Confirm by compiling, then ignore.

### Verification
- [ ] `mvn -o clean verify` green, with every test carried across, not deleted.
- [ ] Live smoke test, the same five-step sequence as phase 1, plus:
      6. A spawn group whose points are in the nether, to exercise the world-name path.
      7. A `language.yml` containing a literal `&` and a colour code in the same message.
- [ ] Upgrade-in-place from the phase 1 jar, confirming kills and groups survive.

### Ship
- [ ] Version 3.0.0, on the same reasoning as phase 1: it drops 1.21.x support.
- [ ] README: supported Minecraft version, Java 25, and a line telling 1.21 users which jar
      is theirs.
- [ ] Keep the phase 1 jar downloadable. Servers move slowly, and 1.21.11 will have a long
      tail.

## Rollback

Harder than phase 1. The server's own world migration to 26.x is one-way, so rolling the plugin
back means rolling the server back to a pre-migration backup. Our own files stay compatible --
`spawns.yml` and `kills.yml` do not change format -- but that is cold comfort if the world
cannot be opened by the older server. Take the backup.
