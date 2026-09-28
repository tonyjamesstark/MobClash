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
| `mobclash.reload` | `/mobclash reload` | op |

Each node covers both forms of its command: `mobclash.summon` grants `/summonmobs` and
`/mobclash summonmobs`. A command is hidden from players who lack its permission. `/mobclash`
itself needs none: its help and tab completion show only the commands the player may run.
Command blocks and the console run everything.
