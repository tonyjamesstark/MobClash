# Examples

## Timed waves from command blocks

```
/addspawn arena                 # repeat at each point
/setchest arena wave1           # chest: 10 zombie eggs
/setchest arena wave2           # chest: 5 zombie, 5 skeleton eggs
/setchest arena boss            # chest: 1 wither egg
```

Then chain command blocks with delays between them:

```
/summonmobs arena wave1 all 3
/summonmobs arena wave2 all 3
/summonmobs arena boss random 1
```

## Setting a wave's mix

The share of each mob is its share of the eggs in the chest. Only the count matters, not how the
eggs are laid out:

```
/setchest arena mixed           # chest: 3 zombie eggs in one slot, 1 skeleton egg
/summonmobs arena mixed random 100
```

That gives about 75 zombies and 25 skeletons. Measured on a test server: 80 and 20. Put the same
3 zombie eggs in three separate slots and nothing changes. With 3 zombie and 3 skeleton eggs,
one stack against three single eggs, 100 summons gave 51 zombies and 49 skeletons.

Each mob is drawn on its own and the eggs stay in the chest, so the chest sets the odds, not the
number of mobs. One wither egg is enough for `/summonmobs arena boss random 3`, and a chest of
64 zombie eggs still gives 1 mob for `/summonmobs arena wave1 random`.

## Boss with adds

```
/addspawn boss_room             # center, then each corner
/setchest boss_room boss        # chest: 1 wither egg
/setchest boss_room adds        # chest: 5 zombie, 5 skeleton eggs
/summonmobs boss_room boss random 1
/summonmobs boss_room adds all 2
```

Whoever lands the killing blow on the wither is announced to the whole server.

## Armed waves

```
/setchest arena wave1           # chest: 10 zombie eggs, 5 skeleton eggs
/setchest arena armory          # chest: 2 iron helmets, 1 diamond chestplate, 3 iron swords
/summonmobs arena wave1 all 3 true      # gear from config.yml
/summonmobs arena wave1 all 3 armory    # gear from the armory chest
```

The armory's helmets are twice as likely as a single item would be. Enchant or rename an item in
the chest and the mobs wear it that way. None of it drops, so players cannot farm the armory.

## Ambushes

```
/addspawn ambush                # at several spots around a base
/setchest ambush easy           # chest: zombie and spider eggs
/summonmobs ambush easy random 5
```

`random` scatters the 5 mobs over the ambush points, so each trigger hits a different mix of
spots.

## Kill race

```
/killboard world on             # everyone here sees the leaderboard
/kills resetall                 # start from zero
/killboard alloff               # hide it afterwards
```
