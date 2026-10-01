# Commands

Every command runs as `/mobclash <command>`. `/mobclash help`, or plain `/mobclash`, lists the
ones you may use, and tab completion offers them. Each one except `reload` and `version` also
runs on its own, as `/summonmobs` or `/kills`, the form the examples below and older command
blocks use.

| Command | What it does | Permission |
|---------|--------------|------------|
| `/mobclash help` | List the commands you can use | none |
| `/mobclash addspawn <group>` | Add your position to a group | `mobclash.addspawn` |
| `/mobclash removespawn <group> [number]` | Remove the group's point nearest you, or the one `/listspawns` shows as that number | `mobclash.removespawn` |
| `/mobclash removegroup <group> [confirm]` | Show what removing a group would forget, or with `confirm` forget its points and chests | `mobclash.removegroup` |
| `/mobclash listgroups` | List groups and their point counts | `mobclash.listgroups` |
| `/mobclash listspawns <group>` | List a group's point coordinates | `mobclash.listspawns` |
| `/mobclash showspawns <group>` | Flash a particle marker at each point | `mobclash.showspawns` |
| `/mobclash setchest <group> <wave>` | Use the chest you are looking at as a wave's egg pool, or as a gear chest | `mobclash.setchest` |
| `/mobclash listchests [group]` | List chests set with `/setchest`, whether they're still there, and their eggs | `mobclash.listchests` |
| `/mobclash removechest <group> <chest>` | Forget a chest set with `/setchest`; the block stays | `mobclash.removechest` |
| `/mobclash summonmobs <group> <wave> <random\|all> [amount] [true\|false\|<gear chest>]` | Summon mobs from a wave, optionally with random equipment | `mobclash.summon` |
| `/mobclash killmobs [group]` | Remove the loaded MobClash mobs, or one group's, with no drops or kill credit | `mobclash.killmobs` |
| `/mobclash kills [top [amount]\|reset\|resetall]` | Your kills, the leaderboard, or a reset | `mobclash.kills` |
| `/mobclash killboard [<on\|off>\|world <on\|off> [world]\|<world> <on\|off>\|alloff]` | Kill leaderboard in the sidebar | `mobclash.killboard` |
| `/mobclash reload` | Re-read `config.yml` and `language.yml` | `mobclash.reload` |
| `/mobclash version` | Show the plugin and server version | `mobclash.version` |

Every command also answers to its plugin-qualified name, for example `/mobclash:addspawn`.
The server's own `/help MobClash` lists the standalone commands. `/mobclash version` is not
standalone, because `/version` is the server's own command.

`/mobclash reload` re-reads both files without restarting the server.
It leaves `spawns.yml` and `kills.yml` alone: the plugin writes those itself.

## Summoning

- `random N` spawns N mobs, each at its own random spawn point.
- `all N` spawns N mobs at every spawn point.
- `amount` is 1-100, default 1. The total is capped by `max-mobs-per-summon`.
- A last argument of `true` or a gear chest's name adds random equipment. See
  [Random equipment](#random-equipment).
- Each mob's type is drawn from the wave's chest, weighted by egg count: 10 zombie eggs and 2
  creeper eggs give about 83% zombies. Only the count matters: a stack of 3 eggs in one slot
  weighs the same as 3 single eggs in three slots. Each mob is drawn independently, so one egg
  is enough for any amount, and the eggs stay in the chest. See
  [Setting a wave's mix](examples.md#setting-a-waves-mix) for measured results.
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

## Removing summoned mobs

```
/killmobs           # every MobClash mob in a loaded chunk
/killmobs arena     # only the mobs summoned for group arena
```

`/killmobs` removes the mobs MobClash summoned and says how many went. The mobs vanish without
dying, so they drop nothing, give no XP, and count as nobody's kill. A group that has no mobs,
or does not exist, removes none. Mobs in unloaded chunks are not reached, so a mob in a far
corner of the world can outlast the command. Run it again once that area is loaded. Other
plugins' and naturally spawned mobs are left alone. It is not `/killall`, since Essentials uses
that name.

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

## Listing chests

`/listchests` is a diagnostic for a summon that silently does nothing: it shows every chest set
with `/setchest`, whether the block is still a chest, and -- if it is -- how many spawn eggs and
total items are in it.

```
/listchests          # every group's chests
/listchests arena    # only that group's chests
```

- Without a group, every group that has a chest is shown; a group with spawn points but no
  chest set is left out. With a group, an unknown name gives the same error as `/listspawns`.
- A chest that is still a `CHEST` block shows its spawn egg count (items whose type ends in
  `_spawn_egg`, summed by stack amount) and its total item count.
- A chest that is no longer a `CHEST` block -- broken, replaced, or never placed where
  `/setchest` recorded it -- shows the block type found there instead.
- Output is sorted by group, then chest name. A wave chest and a gear chest are the same thing
  internally, so both are listed the same way.

A summon that finds no chest where `/setchest` recorded one names the chest, its group, the
world and coordinates, and the block found there. Look at the chest in its new place and run
`/setchest` again, or forget the record with `/removechest`.

## Removing spawn points, chests and groups

```
/removespawn arena              # the arena point nearest you
/listspawns arena               # numbers each point
/removespawn arena 3            # point #3, from anywhere, the console included
/removechest arena wave4        # forget the wave4 chest; the block stays
/removegroup arena              # say what removing arena would forget
/removegroup arena confirm      # forget all of arena's points and chests
```

- `/removespawn <group> <number>` takes the number `/listspawns` shows. The points after it move
  up one number, so run `/listspawns` again before removing a second one. A number outside the
  group's range, or a word, changes nothing and names the valid range.
- A group whose last spawn point is removed is gone from `/listgroups`. Its chests stay until
  `/removechest` or `/removegroup`.
- `/removechest` and `/removegroup` forget records only. The chest blocks and their contents are
  left in the world.
- `/removegroup` without `confirm` changes nothing. It says how many spawn points and chests the
  group has and gives the command that removes them. A group made only with `/setchest` can be
  removed too.

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

`/killboard` toggles a sidebar with the top 10 players in your world by kills, including those
with 0, plus your own line if you are outside them. Kill counts are global; the ranking is just
whoever shares your world right now. It updates on every kill, on a kill reset, and when players
join, leave, or change world. Everyone joins with it off.

```
/killboard                      # toggle for yourself
/killboard on                   # or off - set it for yourself
/killboard world on             # everyone in your world, needs mobclash.killboard.admin
/killboard world on MonsterMash # a named world, case-insensitive; also takes minecraft:monstermash
/killboard MonsterMash on       # shorthand for the line above
/killboard alloff               # off for everyone, in every world; needs mobclash.killboard.admin
```

`world on` switches the board on for that world itself, not just the players in it right now:
anyone who joins the world, or arrives from another one, gets it too. `world off` clears the
switch and hides the board from everyone currently in that world, including anyone who had
turned it on for themselves.

An unrecognised world name gets its own message rather than falling back to the sender's world.

## Command blocks

Command blocks and the console skip permission checks. `/addspawn` and `/removespawn` without
a number use the command block's own position. `/removespawn` with a number, `/removechest` and
`/removegroup` need no position, so they work from the console too, as does `/killmobs`. `/setchest` needs a player, since it uses the chest you look at.
`/killboard world` with no world name uses the command block's own world; the named forms
(`/killboard world <on|off> <world>`, `/killboard <world> <on|off>`) work from the console too,
since they do not need a position. The `/mobclash <command>` forms behave the same way.
