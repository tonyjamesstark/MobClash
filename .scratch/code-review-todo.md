# MobClash code review — work list

Source: three-pass review at `6516c7f` (pstack interrogate panel Opus 5 / Sonnet 5 / Haiku 4.5,
matt pocock two-axis, ponytail over-engineering). Every item below was verified against source
before landing on this list.

Branch: `review-fixes`.

---

## Act On — done

All implemented on `review-fixes` and verified by `mvn clean verify` (49 unit + 8 integration,
all passing). Baseline on `main` before the work: 42 unit passing, all 8 integration **erroring**.

Being implemented now. Findings 1, 2, 3, 9 are one change (the persistence layer); 4 and 5 are
one change (the permission/sender contract).

- [x] **A1. Null world silently poisons config, then save erases it.**
      `SpawnManager.java:43-47,74-78` build `new Location(Bukkit.getWorld(name), ...)` unchecked.
      `saveConfigData` clears `spawn-groups` (line 110) *before* dereferencing `loc.getWorld()`
      (line 118), so the NPE leaves the config tree erased and the next unrelated `saveConfig()`
      flushes the truncated version to disk. Skip-and-warn unresolvable worlds at load; build the
      replacement section detached so the set is atomic.
- [x] **A2. `onDisable` chains two saves unguarded.**
      `MobClashPlugin.java:44-58`. A1's throw aborts before `saveKillData()`, and kill data is
      only ever persisted at shutdown or explicit reset. Wrap each save independently.
- [x] **A3. Group/wave names concatenated into config paths unvalidated.**
      `SpawnManager.java:117,136`. `.` is Bukkit's path separator, so `/addspawn wave.one` nests
      wrongly and reloads as group `wave` with a null world one level too shallow — straight into
      A1, plugin fails to enable. Validate at the command boundary; use `createSection`.
- [x] **A4. Permission gate implemented twice; outer copy makes the bypass decorative.**
      Every command declares `permission:` in `plugin.yml`, and Bukkit's `PluginCommand.execute`
      returns before the executor runs. So `BaseCommand.java:50-61` is unreachable except by
      senders who already passed, the `language.yml` `no-permission` string is dead for all eight
      commands, and command-block support only works because every node is `default: op`.
      Remove the `permission:` keys from the `commands:` block, keep the `permissions:` block.
- [x] **A5. Command-block contract is false as documented.** *(decided: make CB work where sensible)*
      `addspawn`/`removespawn` to use `BlockCommandSender.getBlock().getLocation()` instead of
      requiring a player. `setchest` stays player-only — it needs line of sight — and the README
      is corrected to say so. Also fix `AddSpawnCommandTest.java:112-119`, named
      `testCommandBlockBypassesPermission` while asserting `"§cPlayers only!"`.
- [x] **A6. `/kills top -1` throws, reachable by every player.**
      `KillsCommand.java:51` clamps from above only; `-1` reaches `Stream.limit(-1)`.
      `mobspawner.kills` is `default: true`. `/kills top 0` reports "no kills" with a full board.
      Header also prints the requested limit rather than the row count.
- [x] **A7. `MobClashPluginIT` cannot pass; test counts are fabricated.**
      `setUp` never stubs `plugin.getName()`, which `new MobTracker(plugin)` needs via
      `NamespacedKey`. Failsafe binds the IT to `verify`, so `mvn test` never ran it.
      `TEST-SUMMARY.md:5-6` claims 8 files / 48+ cases; actual is 7 files, 50 `@Test` methods,
      42 of which `mvn test` runs.
- [x] **A8. Persistence has zero real coverage — where A1 and A3 live.**
      Every `SpawnManager` test stubs `config.contains(anyString())` → `false`, so
      `loadConfigData` (77 of 258 lines) does nothing in every test. No `verify(config).set(...)`
      anywhere. Replace the `FileConfiguration` mock with a real `YamlConfiguration` on `@TempDir`
      and assert a save → load round trip, including the unloaded-world and dotted-name cases.
- [x] **A9. Location serialization hand-written four times.**
      `SpawnManager.java:43-47,74-78` (read), `118-121,137-140` (write). The structural reason A1
      needs fixing in four places instead of one.
- [x] Correct the false coverage claims in `TEST-SUMMARY.md` (loot-to-inventory "covered in
      integration tests" when `MobDeathListener` has no test; `Commands ✅ Good` while
      `KillsCommand` has none).

---

## Deferred — conditional approval

Approved to implement **without asking again**, each when its stated condition is met. Until then
they stay here. If a condition is already true when you read this, it is approved now.

### Correctness, blocked only on wanting the behavior change

- [x] **Cap total entities per `/summonmobs`.** `parseAmount` bounds `amount` to 1-100, then `all`
      mode spawns `amount × locations.size()` synchronously. 100 points × 100 = 10,000
      `spawnEntity` calls in one tick, reachable from a redstone clock at 20 Hz.
      **Condition:** approved as soon as any group exceeds ~20 spawn points, or on the first
      report of tick lag from a summon. Cap the product, not the factor.
- [x] **`handleLootToInventory` uses the wrong leftover stacks.** `MobDeathListener.java:63-66`
      re-adds the original `drop` rather than what `addItem` returned; correct today only because
      CraftBukkit mutates the passed stack in place. Line 73 counts stacks not items, so 64 rotten
      flesh with 10 fitting logs "Added 0 items".
      **Condition:** approved now if `loot-to-inventory` is ever set true in production; otherwise
      on any Paper/CraftBukkit major upgrade, whichever comes first.
- [x] **Spawn-egg → `EntityType` mapping via enum-name string surgery behind an empty catch.**
      `SummonMobsCommand.java:129-147`. Unmappable eggs vanish with no log line at all, leaving a
      partially-correct mob pool and nothing to debug from. Namespaced keys are the stable
      identity. **Condition:** the silent `catch` gets a `WARNING` log immediately — approved
      unconditionally as a one-line change. The full registry-key rewrite is approved on the first
      report of an egg that does not spawn.
- [x] **`spawnEntity` result never checked.** A cancelled `CreatureSpawnEvent` (WorldGuard,
      anti-lag plugins) still returns the entity object, so the plugin reports "Spawned 100
      mob(s)" over an empty arena and tags discarded entities.
      **Condition:** approved when the plugin is next run on a server with region protection.
- [x] **`/kills resetall` does its own `hasPermission` outside the bypass.** `KillsCommand.java:94`.
      **Condition:** approved together with A4 if that change lands cleanly; otherwise when
      command-block use of `resetall` is actually wanted.
- [x] **`MobTracker.loadKillData` null hole.** `MobTracker.java:82` dereferences
      `getConfigurationSection` unchecked, and `contains` is true for a scalar too, so a hand edit
      or a crash mid-write fails `onEnable`. Sibling `SpawnManager` guards this exact pattern six
      times. Cast at line 86 should be `((Number) v).intValue()`; dropped player data logs at INFO.
      **Condition:** approved now — it is the same class of bug as A1 and costs four lines.
      *(Still open: A4's fix covered the `resetall` bypass only, not this.)*

### Structural, wanted but not urgent

- [x] **Split runtime state out of `config.yml`.** Done: `spawns.yml` and `kills.yml`, with a
      one-time migration on first start after upgrade.
      *The stated reason was wrong.* `YamlConfiguration.options().parseComments()` defaults to
      true on 1.20.4 — probed directly — so `saveConfig()` preserves comments and always did.
      The real defect is that every `/addspawn` rewrote the operator's settings file from the
      plugin's in-memory copy, so an edit to `config.yml` on a running server was silently
      discarded at the next command. Separating the files also stops a failed write to one from
      taking the other with it.
- [x] **Collapse the duplicated spawn loop.** `SummonMobsCommand.java:176-201` — both branches
      become one loop over a `targets` list, which also deletes the `getRandom()` accessor and
      resolves `getRandomSpawnPoint` (dead in production; one caller, a test).
      **Condition:** approved on next touch of `SummonMobsCommand` for any reason.
- [x] **Stop leaking mutable internal collections.** `SpawnManager.java:227-245` returns live
      lists, letting callers mutate persisted state around save-on-mutation.
      **Condition:** approved the moment a second caller of `getSpawnPoints`/`getGroupWaves`
      appears.
- [x] **`/showspawns` spawns real `AreaEffectCloud` entities for a "particle marker"** —
      visible to `@e` selectors, fires `EntitySpawnEvent` server-wide, written into the chunk if
      it saves. `setColor(Color.RED)` at line 47 is inert because `FLAME` is not colourable.
      **Condition:** approved on any report of stray entities or `@e` selector interference.
- [x] **`BaseCommand` types its plugin as `JavaPlugin`,** forcing eleven `((MobClashPlugin) plugin)`
      casts. **Condition:** approved on next touch of `BaseCommand`.
- [x] **`registerCommands` is eight copies with no null check** — `getCommand("x")` returns null
      for any name missing from `plugin.yml`, giving a bare NPE during `onEnable`.
      **Condition:** approved whenever a ninth command is added.
- [~] **Accept `TRAPPED_CHEST` in `SetChestCommand.java:36`.** **Declined.** Wave chests stay
      plain `CHEST` only. Not deferred — asked and answered, so nothing here is waiting on a
      condition. Reopen only if that preference changes.

### Test quality

- [x] **Negative-path tests cannot distinguish which error fired.** All `getMessage` overloads
      stubbed to `"Message"`, then `verify(player).sendMessage(anyString())`. Four tests in
      `SummonMobsCommandTest` make byte-identical assertions and pass if the guard clauses are
      transposed. `AddSpawnCommandTest` already shows the better pattern.
      **Condition:** approved as each of those branches is next modified.
- [x] **Untested branches:** `parseAmount` bounds, `invalid-mode`, `chest-missing`,
      `no-spawn-eggs`, and the silent skip in `getSpawnEggsFromChest`.
      **Condition:** approved together with A8 if the round-trip harness makes them cheap.
- [x] **`@Mock(lenient = true)` everywhere** suppresses exactly the unused-stub signal that
      reveals untested paths — and `TEST-SUMMARY.md:195-198` documents it as a best practice.
      **Condition:** approved when the suite is next reworked; not worth a standalone pass.
- [x] **`SummonMobsCommandTest.java:99` stubs `chestLoc.getBlock()` on a real `Location`** — it
      only works because Mockito catches the inner `world.getBlockAt` call, so the line means
      something entirely different from what it reads as. Line 98's stub is dead.
      **Condition:** approved on next touch of that file.

### Identity and packaging

- [x] **`plugin.yml` hardcodes `version: 1.0.0` while the pom is `1.1.0`,** so `/version MobClash`
      misreports. Resource filtering is already enabled (`pom.xml:64-69`) and unused;
      `${project.version}` costs one character. `plugin.yml:5` still says `author: YourName`.
      README:48-49 names the wrong jar. **Condition:** approved now — trivial and user-visible.
      *(Folded into the Act On pass since it is one line and A7 already touches the docs.)*
- [x] **`pom.xml:5` groupId is still `com.example`;** README:260-270 documents the package as
      `com.example.mobclash` when it is `io.tjs.mobclash`.
      **Condition:** approved now for the README; the groupId change is approved only before the
      first published release, since it moves the artifact coordinates.
- [x] **Shade plugin has nothing to shade** (`pom.xml:165-196`) — `paper-api` is `provided`, junit
      and mockito are `test`, and the `*Test.class`/`*IT.class` excludes are no-ops.
      **Condition:** approved now unless a runtime dependency is planned; ~24 lines deleted.
- [x] **Permission namespace is `mobspawner.*` while the plugin is MobClash** — a rename artifact
      now frozen into an operator-facing contract. Commands are also unnamespaced with no
      `aliases:`, so `/kills` will collide with other plugins.
      **Condition:** decide before 1.2.0 ships. Changing it after release breaks every permissions
      config, so this is approved *only* as part of a deliberate major-version migration.
