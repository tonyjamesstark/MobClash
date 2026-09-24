# MobClash

A Paper plugin for arena-style mob waves. Mark spawn points in named groups, fill a chest with
spawn eggs for each wave, then summon a wave by command or command block.

## Features

- 🎯 **Spawn groups** - any number of groups, each with many spawn points
- 🌊 **Waves** - one chest of spawn eggs per wave, weighted by egg count
- 🎲 **Two modes** - scatter mobs over random points, or spawn at every point
- 📊 **Kill tracking** - `/kills` leaderboard and an optional sidebar
- 🤖 **Command blocks** - run every command except `/setchest`, with no permissions needed
- 🌍 **Translatable** - every message lives in `language.yml`

## Requirements

Paper 1.21.11 (or a fork) on Java 21. For 1.20.x, use the v1.2.0 jar.

## Install

1. Download `mobclash-2.0.0.jar` from releases.
2. Put it in `plugins/` and restart the server.

## Quick start

```
/addspawn arena                     # at each spot mobs should appear
/setchest arena wave1               # while looking at a chest of spawn eggs
/summonmobs arena wave1 random 5    # 5 mobs, each at a random point
/summonmobs arena wave1 all 3       # 3 mobs at every point
/killboard                          # show the kill leaderboard in your sidebar
```

## Documentation

- [Commands](docs/commands.md) - every command, summoning modes, kills, the sidebar
- [Permissions](docs/permissions.md) - nodes and defaults
- [Configuration](docs/configuration.md) - `config.yml`, data files, `language.yml`
- [Examples](docs/examples.md) - timed waves, boss fights, ambushes, kill races
- [Troubleshooting](docs/troubleshooting.md)
- [Development](docs/development.md) - building and testing
- [Changelog](CHANGELOG.md)

## License

This plugin is provided as-is for use on Minecraft servers.

---

Note from the author: 

the vast majority of this was created as a test case for exploring AI coding workflows. Results may vary. Verification of the project is in progress....
