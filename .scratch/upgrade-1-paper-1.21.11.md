# Phase 1: MobClash on Paper 1.21.11

From `1.20.4-R0.1-SNAPSHOT` (current, shipping as v1.2.0) to `1.21.11-R0.1-SNAPSHOT`.

Phase 2 (`.scratch/upgrade-2-paper-26.2.md`) starts from the finished state of this document.
Do not merge the two. They need different JDKs, and the Adventure work in phase 2 is large
enough that mixing it with this would make a bisect useless.

## What makes this non-trivial

Not the Minecraft version. The toolchain: paper-api 1.21.11 ships bytecode 65, so the whole
build moves to **Java 21**, and this project currently pins Java 17 because google-java-format
1.19.2 breaks on newer JDKs. The JDK move is the first task and the one most likely to stall.

The API surface this plugin touches is small, and the riskiest single change is a quiet one:
`NamespacedKey(Plugin, String)` now derives its namespace from `plugin.namespace()` rather than
`plugin.getName()`. That key is what marks every mob we have ever spawned.

## Prerequisites

- [ ] **Java 21 installed.** `sdk install java 21.0.5-tem` or similar — tooling installs are
      yours to run, so this is a request, not a step I take.
- [ ] Confirm google-java-format works on 21. 1.19.2 is current here; if spotless fails, bump
      `google-java-format` before touching any source, so formatting noise stays out of the
      real diff.
- [ ] A test server running Paper 1.21.11, with a copy of a real `spawns.yml`/`kills.yml`.
- [ ] Branch `upgrade/paper-1.21.11` off `main` at v1.2.0.

## Checklist

### Toolchain
- [ ] `pom.xml`: `maven.compiler.release` 17 -> 21.
- [ ] `pom.xml`: paper-api `1.20.4-R0.1-SNAPSHOT` -> `1.21.11-R0.1-SNAPSHOT`.
- [ ] `plugin.yml`: `api-version: 1.20` -> `1.21`.
- [ ] Do **not** add an explicit `net.kyori:adventure-api` dependency. Pinning it is the most
      common 1.21.11 migration failure; paper-api brings 4.26.1 transitively and an older pin
      fails to compile. We have no pin today — keep it that way.
- [ ] Bump Mockito if 5.7.0 misbehaves on 21. Check before assuming; it may be fine.
- [ ] `mvn -o clean verify` compiles. Expect failures; fix them below, not by chasing the
      first stack trace.

### The one that can lose data
- [ ] **Pin the mob tag key explicitly.** `MobTracker.java:24` builds
      `new NamespacedKey(plugin, "mobclash_spawned")`. On 1.21.11 that constructor reads
      `plugin.namespace()` instead of `plugin.getName().toLowerCase()`. If the two ever differ,
      every mob tagged by an older version stops being recognised: `isMobClashMob` returns
      false, those mobs drop out of kill tracking silently, and nothing in the log says why.
      Replace it with the two-string constructor naming the namespace literally:
      `new NamespacedKey("mobclash", "mobclash_spawned")`. Same key as today, immune to the
      change, and immune to a future rename of the plugin.
- [ ] Add a test asserting the key's exact namespace and value, so the constant cannot drift.
- [ ] On the test server: spawn mobs on v1.2.0, upgrade the jar in place, kill them, confirm
      the kills still count. This is the check that proves the tag survived.

### API deltas to work through
- [ ] `Particle.FLAME` (`ShowSpawnsCommand.java:43`). 1.21 renamed several constants
      (`VILLAGER_HAPPY` -> `HAPPY_VILLAGER`, `SMOKE_NORMAL` -> `SMOKE`). `FLAME` is expected to
      be unchanged — confirm at compile, do not assume.
- [ ] `EntityType.values()` and `type.getKey().getKey()`
      (`SummonMobsCommand.java:150-152`). EntityType is moving toward a registry. If `values()`
      is deprecated, keep the key-based map and change only how the collection is obtained.
      **Do not switch to `Registry.ENTITY_TYPE`**: its static initialiser needs a running
      server and cannot be touched from a unit test. That is why this code looks the way it
      does; the comment at line 142 explains it and should stay.
- [ ] `Material.getKey()` (`SummonMobsCommand.java:161`). Same treatment.
- [ ] `SummonMobsCommandTest` references `EntityType.MUSHROOM_COW` directly. If 1.21 renamed
      that constant to match its vanilla key, the test breaks while production code does not —
      which is the point of writing the mapping against keys. Update the test, keep the intent.
- [ ] Spawn eggs added since 1.20.4 (armadillo, breeze, bogged, creaking) need no code change:
      the key-based map picks up anything the server declares. Worth a line in the release note
      as a genuine gain, not a claim to verify.

### Not our problem, stated so nobody spends a day on it
- Gamerule camelCase -> snake_case: we use no gamerules.
- `PlayerDeathEvent`'s new `DamageSource` constructor: we listen to `EntityDeathEvent`.
- `Attribute.GENERIC_MAX_HEALTH` rename: we read no attributes.
- Unobfuscated server jars in 26.1: we call no NMS, so the remapper change is inert for us.
  It matters in phase 2 only as a reason the ecosystem around us churns.
- [ ] `sendMessage(String)` is deprecated in favour of Adventure Components but still present
      on 1.21.11. **Leave all 50 call sites alone in this phase.** They are phase 2's work.

### Verification
- [ ] `mvn -o clean verify` green: 73 unit, 8 integration.
- [ ] `PluginYmlTest` still passes — it reads the filtered descriptor, so the `api-version`
      bump goes through it.
- [ ] Live smoke test on Paper 1.21.11, in this order:
      1. Fresh install: `/addspawn`, `/setchest`, `/summonmobs`, `/kills`.
      2. Upgrade-in-place over a v1.2.0 data folder: confirm groups and kills load.
      3. A command block runs `/addspawn` and `/summonmobs` with no permissions granted.
      4. `/help addspawn` and `/help mobclash:addspawn` both show the right label.
      5. A wave chest holding a mooshroom egg and a 1.21-only egg both spawn.
- [ ] Watch the console for the migration line and for any `WARNING` from `SpawnManager`.

### Ship
- [ ] Decide the version. Recommendation: **2.0.0**. It drops 1.20.x support, which is a
      breaking change for anyone running the old jar, and the major number is the only honest
      signal of that.
- [ ] README: state the supported Minecraft version and the Java requirement.
- [ ] Release note, tag, and the same manual-release flow as v1.2.0.

## Rollback

v1.2.0's jar stays compatible with 1.20.4 servers. Nothing in this phase changes the on-disk
format of `spawns.yml` or `kills.yml`, so downgrading the jar is safe as long as the
`NamespacedKey` namespace did not change — which is exactly what pinning it guarantees.
