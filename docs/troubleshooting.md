# Troubleshooting

**Mobs not spawning.**
- `/listgroups` to check the group exists and has points.
- `/showspawns <group>` to see where the points are.
- Check the wave's chest still exists and holds spawn eggs.
- Look in the console. Set `logging-level: INFO` for detail.

**Permission denied, or the command is missing.** Grant the node from
[permissions.md](permissions.md), or run it from a command block.

**Configuration not loading.** Check the console for YAML errors and that the file is UTF-8.
Deleting `config.yml` regenerates the defaults.
