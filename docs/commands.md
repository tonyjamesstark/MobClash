# Commands

| Command | What it does | Permission |
|---------|--------------|------------|
| `/addspawn <group>` | Add your position to a group | `mobclash.addspawn` |
| `/removespawn <group>` | Remove the group's point nearest you | `mobclash.removespawn` |
| `/listgroups` | List groups and their point counts | `mobclash.listgroups` |
| `/listspawns <group>` | List a group's point coordinates | `mobclash.listspawns` |
| `/showspawns <group>` | Flash a particle marker at each point | `mobclash.showspawns` |
| `/setchest <group> <wave>` | Use the chest you are looking at as a wave's egg pool | `mobclash.setchest` |
| `/summonmobs <group> <wave> <random\|all> [amount]` | Summon mobs from a wave | `mobclash.summon` |
| `/kills [top [amount]\|reset\|resetall]` | Your kills, the leaderboard, or a reset | `mobclash.kills` |
| `/killboard [world <on\|off>\|alloff]` | Kill leaderboard in the sidebar | `mobclash.killboard` |

Every command also answers to its plugin-qualified name, for example `/mobclash:addspawn`.

## Summoning

- `random N` spawns N mobs, each at its own random spawn point.
- `all N` spawns N mobs at every spawn point.
- `amount` is 1-100, default 1. The total is capped by `max-mobs-per-summon`.
- Each mob's type is drawn from the wave's chest, weighted by egg count: 10 zombie eggs and 2
  creeper eggs give about 83% zombies.

```
/summonmobs arena wave1 random 5    # 5 mobs scattered over the points
/summonmobs arena wave2 all 3       # 3 mobs at every point
```

## Kills

```
/kills               # your own count
/kills top 20        # leaderboard, default 10, max 50
/kills reset         # clear your own
/kills resetall      # clear everyone's, needs mobclash.kills.resetall
```

## Kill leaderboard sidebar

`/killboard` toggles a sidebar with the top 10 killers, plus your own line if you are outside
them. It updates on every kill and reset. Everyone joins with it off.

```
/killboard                # toggle for yourself
/killboard world on       # everyone in your world, needs mobclash.killboard.admin
/killboard alloff         # off for everyone, needs mobclash.killboard.admin
```

`world` acts on whoever is in the world at that moment. It does not follow players who change
world later.

## Command blocks

Command blocks and the console skip permission checks. `/addspawn` and `/removespawn` use the
command block's own position. `/setchest` needs a player, since it uses the chest you look at.
`/killboard world` uses the command block's world.
