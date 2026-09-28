# Commands

Every command runs as `/mobclash <command>`. `/mobclash help`, or plain `/mobclash`, lists the
ones you may use, and tab completion offers them. Each one except `reload` also runs on its own,
as `/summonmobs` or `/kills`, the form the examples below and older command blocks use.

| Command | What it does | Permission |
|---------|--------------|------------|
| `/mobclash help` | List the commands you can use | none |
| `/mobclash addspawn <group>` | Add your position to a group | `mobclash.addspawn` |
| `/mobclash removespawn <group>` | Remove the group's point nearest you | `mobclash.removespawn` |
| `/mobclash listgroups` | List groups and their point counts | `mobclash.listgroups` |
| `/mobclash listspawns <group>` | List a group's point coordinates | `mobclash.listspawns` |
| `/mobclash showspawns <group>` | Flash a particle marker at each point | `mobclash.showspawns` |
| `/mobclash setchest <group> <wave>` | Use the chest you are looking at as a wave's egg pool, or as a gear chest | `mobclash.setchest` |
| `/mobclash summonmobs <group> <wave> <random\|all> [amount] [true\|false\|<gear chest>]` | Summon mobs from a wave, optionally with random equipment | `mobclash.summon` |
| `/mobclash kills [top [amount]\|reset\|resetall]` | Your kills, the leaderboard, or a reset | `mobclash.kills` |
| `/mobclash killboard [world <on\|off>\|alloff]` | Kill leaderboard in the sidebar | `mobclash.killboard` |
| `/mobclash reload` | Re-read `config.yml` and `language.yml` | `mobclash.reload` |

Every command also answers to its plugin-qualified name, for example `/mobclash:addspawn`.
The server's own `/help MobClash` lists the standalone commands.

`/mobclash reload` re-reads both files without restarting the server.
It leaves `spawns.yml` and `kills.yml` alone: the plugin writes those itself.

## Summoning

- `random N` spawns N mobs, each at its own random spawn point.
- `all N` spawns N mobs at every spawn point.
- `amount` is 1-100, default 1. The total is capped by `max-mobs-per-summon`.
- A last argument of `true` or a gear chest's name adds random equipment. See
  [Random equipment](#random-equipment).
- Each mob's type is drawn from the wave's chest, weighted by egg count: 10 zombie eggs and 2
  creeper eggs give about 83% zombies.
- An egg's `entity_data` is kept, so eggs from `/give` with custom attributes, health, loot
  table or persistence summon that mob. The egg's display name is not applied.
- Without random equipment, a mob from an egg with `entity_data` gets no spawn gear, as with
  `/summon` and NBT. A skeleton then needs `equipment:{mainhand:{id:"minecraft:bow"}}` to have a
  bow. Gear the egg gives never drops. Plain eggs spawn vanilla mobs with vanilla gear and drop
  chances.
- An egg is skipped, with a warning to the console and to whoever ran the summon, if its
  `entity_data` spawns something other than a mob, has riders (`Passengers`) or fixes a `UUID`.

```
/summonmobs arena wave1 random 5    # 5 mobs scattered over the points
/summonmobs arena wave2 all 3       # 3 mobs at every point
```

## Random equipment

The last argument turns on random equipment. `false` is the default. `true` draws from the
lists in `config.yml`, and the name of a chest set with `/setchest` draws from that chest.

```
/summonmobs arena wave1 all 5 true     # gear from config.yml
/summonmobs arena wave1 random true    # amount left out, so 1
/setchest arena gear                   # while looking at a chest of armor and weapons
/summonmobs arena wave1 all 5 gear     # gear from that chest
```

- Zombies, husks and zombie villagers draw armor and a weapon. Skeletons, strays, bogged and
  parched draw armor and always hold a bow; the weapon list and chest weapons never reach them.
- The default weapons are copper, iron and golden swords, shovels and axes. `config.yml` sets
  the lists and the enchantment levels; see [Configuration](configuration.md).
- The odds are vanilla's on hard difficulty. 15% of mobs get armor, from one tier, as boots
  first and then each further piece at 90%. Lower tiers are more common. 5% of zombies get a
  weapon.
- Armor can get Protection and weapons Sharpness, at levels up to `max-protection` and
  `max-sharpness`. Swords and axes take Sharpness; shovels do not.
- Nothing a summoned mob wears or holds drops, including a skeleton's bow.
- A plain egg's vanilla gear is replaced. A mob from an egg with `entity_data` keeps the gear
  the egg gives, and only its empty slots are filled.
- A gear chest is any chest set with `/setchest <group> <name>`, in the same group as the wave.
  In it, each armor piece goes to its own slot, and every other item is a zombie
  weapon. A stack of three counts three times. Items keep their names, trims, dye and
  enchantments. An item that is already enchanted gets no extra enchantment. The chest is
  read, never emptied.
- Other mobs, such as creepers and spiders, get no gear.

## Kills

```
/kills               # your own count
/kills top 20        # leaderboard, default 10, max 50
/kills reset         # clear your own
/kills resetall      # clear everyone's, needs mobclash.kills.resetall
```

A kill counts when a player kills a mob MobClash summoned. When that mob is a wither or warden,
every player on the server, in every world, sees who killed it. `announce-kills` in
`config.yml` sets which mobs are announced; see [Configuration](configuration.md).

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
`/killboard world` uses the command block's world. The `/mobclash <command>` forms behave the
same way.
