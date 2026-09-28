# Troubleshooting

**Mobs not spawning.**
- `/listgroups` to check the group exists and has points.
- `/showspawns <group>` to see where the points are.
- Check the wave's chest still exists and holds spawn eggs.
- Look in the console. Set `logging-level: INFO` for detail.

**Summoned mobs have little or no gear.** That is by design: 15% of mobs get armor and 5% of
zombies a weapon, as in vanilla. Only zombies, husks, zombie villagers, skeletons, strays,
bogged and parched are equipped. Check the console for warnings about unknown entries in
`random-equipment`. A gear chest name must be a chest set with `/setchest` in the same group.

**The usage message has no equipment argument.** Your `language.yml` predates it. Delete the
`summonmobs-usage` line and run `/mobclash reload`.

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
