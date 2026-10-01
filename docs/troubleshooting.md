# Troubleshooting

**Mobs not spawning.**
- `/listgroups` to check the group exists and has points.
- `/showspawns <group>` to see where the points are.
- Check the wave's chest still exists and holds spawn eggs.
- Look in the console. Set `logging-level: INFO` for detail.

**A wave summons a different number of mobs than there are eggs.** The amount in the command
sets how many mobs come, 1 by default. The eggs only set the odds of each mob type, and they
stay in the chest. Add eggs of a type to make it more common; stacked or in separate slots
counts the same. See [Setting a wave's mix](examples.md#setting-a-waves-mix).

**A summon says a chest is gone.** The chest was moved or broken after `/setchest` recorded it.
The message names the group, the chest, its world and coordinates, and the block now there.
Run `/listchests` to see every chest that is still missing. For each one, look at the chest in
its new place and run `/setchest <group> <chest>`, or run `/removechest <group> <chest>` if it
is no longer needed. Both apply to gear chests too.

**Summoned mobs have little or no gear.** That is by design: 15% of mobs get armor and 5% of
zombies a weapon, as in vanilla. Only zombies, husks, zombie villagers, skeletons, strays,
bogged and parched are equipped. Check the console for warnings about unknown entries in
`random-equipment`. A gear chest name must be a chest set with `/setchest` in the same group.

**Permission denied, or the command is missing.** A command you lack the permission for is left
out of `/mobclash help` and tab completion. Typed on its own it answers "Unknown command", and
as `/mobclash <command>` it gives the no-permission message. Grant the node from
[permissions.md](permissions.md), or run it from a command block.

**Players lost their commands after upgrading to 2.1.0.** The old `mobspawner.*` nodes were
removed. Grant the matching `mobclash.*` nodes instead.

**A wither or warden kill was not announced.** Only a player's kill of a mob MobClash summoned is
announced, and only for mobs in `announce-kills`. A death from lava, a fall or another mob has
no player killer. Check the list in `config.yml` for a misspelled name, which is ignored.

**Configuration not loading.** Check the console for YAML errors and that the file is UTF-8.
Deleting `config.yml` regenerates the defaults. Edits apply after `/mobclash reload`, which
reloads nothing if either file has a syntax error.
