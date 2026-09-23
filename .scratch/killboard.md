# Kill count scoreboard (`/killboard`)

A sidebar that shows the MobClash kill leaderboard, switched on and off by a separate command.

## Behaviour

- `/killboard` toggles the sidebar for the player running it. Permission `mobclash.killboard`,
  default true, same as `/kills`.
- `/killboard world <on|off>` switches it for every player in the sender's current world.
  Works from a command block (its block's world); console has no world and gets `no-location`.
  Permission `mobclash.killboard.admin`, default op.
- `/killboard alloff` turns it off for everyone in every world. Same admin permission.
- The sidebar shows the top 10 killers plus the viewer's own line when they are outside the top
  10. Scores are the kill counts; the sidebar sorts them. 11 lines at most, under the 15 cap.
- It updates on every kill, `/kills reset`, and `/kills resetall`.
- State is in memory only. Quitting forgets it; a player rejoins with it off.
- `world on` applies to whoever is in the world when it runs. It does not follow players who
  change world afterwards, and does not switch on for players who arrive later.

## Design

`KillBoard` (managers/) owns one map, `UUID -> Viewer(Scoreboard ours, Scoreboard previous)`.
Membership is the on/off state, so there is no second flag to keep in sync. Each viewer gets
their own `Scoreboard`, because the "own line" differs per viewer. `previous` is whatever
scoreboard the player had, restored on hide, so turning ours off does not wipe another plugin's.

`refresh()` computes the top 10 once, resolves names once, then renders each viewer. A render
resets only entries that left the board, so the sidebar does not flicker on every kill.

It also listens for `PlayerQuitEvent` to forget the viewer, and `onDisable` hides everyone so a
`/reload` does not leave players holding a dead scoreboard.

Refresh is called from the three places kills change: `MobDeathListener` after `recordKill`,
and `KillsCommand` after `reset` and `resetall`.

## Checklist

- [x] `KillBoard`: show / hide / toggle / hideAll / refresh, quit listener
- [x] `KillBoardCommand`: self toggle, `world on|off`, `alloff`
- [x] Wire into `MobClashPlugin`: construct, register command and listener, hide all on disable
- [x] Refresh from `MobDeathListener` and `KillsCommand`
- [x] `plugin.yml`: command, `mobclash.killboard`, `mobclash.killboard.admin`, wildcard children,
      deprecated `mobspawner.*` aliases (`PluginYmlTest` enforces all three)
- [x] `language.yml`: title, usage, on/off, world, alloff messages
- [x] Unit tests: `KillBoardTest`, `KillBoardCommandTest`
- [x] `mvn -o clean verify` green, spotless clean
- [x] Live on `~/mcserver` instance `mobclash` with the mineflayer bot reading the sidebar:
      toggle on, kill, own line, toggle off restores, `world on`, `alloff`, rejoin is off
      Done 2026-09-23 on Purpur 1.21.11, two mineflayer players (Clasher with kills, Newbie
      without), reading the sidebar from the score packets. Passed: rejoin starts off; on shows
      `Clasher=3, Newbie=0`; a kill redraws to `Clasher=4` live; toggle off clears it;
      `world on` refused without op, then reaches both; `/kills reset` drops Clasher's line
      from Newbie's board; console `alloff` clears both; console `world on` gives
      `no-location`; with Newbie in the Nether, `world on` from the overworld reaches only
      Clasher. Not run: `/reload`, `/kills resetall` (same refresh call as `reset`).
- [x] README: command table and a short section

## Found on the way

- [x] The data-folder `language.yml` is never merged, so every key a release adds showed as
      "Missing translation" on upgraded servers, starting with the killboard title. Fixed in
      `LanguageManager` by using the bundled copy as defaults; confirmed live, since the test
      server still holds the 1.2.0 copy.

## Skipped from the pstack Feature playbook

- architect fan-out and a delegated implementer. CLAUDE.md limits delegation to splitting a
  complex task; this is one coupled change over a handful of files.
