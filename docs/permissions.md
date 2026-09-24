# Permissions

| Permission | Grants | Default |
|------------|--------|---------|
| `mobclash.*` | Everything below | op |
| `mobclash.addspawn` | `/addspawn` | op |
| `mobclash.removespawn` | `/removespawn` | op |
| `mobclash.listgroups` | `/listgroups` | op |
| `mobclash.listspawns` | `/listspawns` | op |
| `mobclash.showspawns` | `/showspawns` | op |
| `mobclash.setchest` | `/setchest` | op |
| `mobclash.summon` | `/summonmobs` | op |
| `mobclash.kills` | `/kills`, `/kills top`, `/kills reset` | everyone |
| `mobclash.kills.resetall` | `/kills resetall` | op |
| `mobclash.killboard` | `/killboard` for yourself | everyone |
| `mobclash.killboard.admin` | `/killboard world` and `/killboard alloff` | op |

A command is hidden from players who lack its permission. Command blocks and the console run
everything.

The old `mobspawner.*` nodes still work. Each is a parent of the matching `mobclash.*` node and
defaults to `false`. New grants should use `mobclash.*`.
