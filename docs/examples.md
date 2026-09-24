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

## Boss with adds

```
/addspawn boss_room             # center, then each corner
/setchest boss_room boss        # chest: 1 wither egg
/setchest boss_room adds        # chest: 5 zombie, 5 skeleton eggs
/summonmobs boss_room boss random 1
/summonmobs boss_room adds all 2
```

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
