# MobClash v2.0.0

Paste this into the GitHub release body. Attach `target/mobclash-2.0.0.jar`.

---

Built and tested against Paper 1.21.11 on Java 21.

## Before you upgrade

**This release needs Paper 1.21.11 and Java 21.** It will not load on 1.20.x. If you are staying
on 1.20.x, keep the v1.2.0 jar. That is the reason for the major version.

Your data carries over as it is. Spawn groups, wave chests and kill counts load unchanged, and
mobs spawned under 1.2.0 still count towards kills after the upgrade.

## New

**`/killboard` puts the kill leaderboard in the sidebar.** It shows the top 10 killers, plus your
own line if you are outside the top 10, and updates on every kill and reset.

- `/killboard` turns it on or off for yourself.
- `/killboard world on|off` switches it for everyone in your current world. From a command
  block, that is the block's world.
- `/killboard alloff` turns it off for everyone in every world.

Everyone joins with it off. Turning it off gives you back whatever scoreboard you had before.
Two new permissions go with it:

- `mobclash.killboard` lets a player toggle their own sidebar. It defaults to everyone.
- `mobclash.killboard.admin` is needed for `world` and `alloff`. It defaults to op.

## Changes

- **`/summonmobs <group> <wave> random N` scatters the mobs.** Each mob now picks its own random
  spawn point. Previously all N went to one point. `all` mode is unchanged.
- **Commands you cannot use are hidden.** Players no longer see commands they lack the
  permission for in tab completion. Typing one gives "Unknown command". Command blocks and the
  console still run everything.

## Fixes

- New messages no longer show as "Missing translation" on a server that has been upgraded.
  `language.yml` is written once, by the first version you ran, and was never updated. Any key
  it lacks now comes from the built-in copy. Your own edits still take priority.

## Tests

95 unit tests and 8 integration tests, all passing. Checked live on a Purpur 1.21.11 server. The
live check covered:

- an in-place upgrade over a 1.2.0 data folder
- command blocks running `/addspawn` and `/summonmobs`
- a wave chest with both a mooshroom egg and a bogged egg, which is new in 1.21
- `/killboard` with two players across two worlds
- the command list a non-op player sees

## Install

1. Make sure the server runs Paper 1.21.11, or a fork of it, on Java 21.
2. Stop the server.
3. Replace the old jar in `plugins/` with `mobclash-2.0.0.jar`.
4. Start the server.
