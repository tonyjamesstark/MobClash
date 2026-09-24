# Configuration

All files live in `plugins/MobClash/`.

## config.yml

```yaml
# INFO (verbose), WARNING (quiet), SEVERE (errors only), OFF (silent)
logging-level: INFO

# Put mob drops straight into the killer's inventory. What does not fit still drops.
loot-to-inventory: false

# Most mobs one /summonmobs may spawn. In "all" mode the total is amount x spawn points.
# 0 disables the cap.
max-mobs-per-summon: 500
```

`config.yml` is yours. The plugin never writes to it, so edits on a running server are kept.

## spawns.yml and kills.yml

The plugin writes spawn groups and wave chests to `spawns.yml`, and kill counts to `kills.yml`.
It rewrites them from memory on every save, so edit them only with the server stopped.

## language.yml

Every message can be changed or translated. `{0}`, `{1}` are placeholders, `&` starts a color
code.

```yaml
no-permission: "&cYou don't have permission to use this command."
addspawn-success: "&aAdded spawn point to group '{0}'! Total points: {1}"
```

The file is written once and never overwritten. Any message it lacks, such as one added by a
later release, comes from the plugin's built-in copy.
