# MobClash

A powerful Minecraft Paper plugin for managing configurable mob spawn groups with per-group customization and flexible spawning modes.

⚔️ **Clash with endless possibilities!**

## Features

- 🎯 **Multiple Spawn Groups** - Create unlimited spawn groups, each with multiple spawn points
- 📦 **Per-Group Mob Pools** - Each group has its own chest of spawn eggs for different mob variety
- 🎲 **Flexible Spawning** - Spawn at one random point or all points simultaneously
- 👁️ **Visual Markers** - Display spawn points with colorful particle clouds
- 🤖 **Command Block Support** - All commands work from command blocks without permission checks
- 🌍 **Multi-Language** - Fully translatable message system
- 📊 **Configurable Logging** - Adjust verbosity from detailed to silent
- ✅ **Fully Tested** - Comprehensive unit and integration test suite

## Requirements

- Minecraft Server: Paper 1.20.4+ (or compatible forks)
- Java: 17+
- Maven: 3.6+ (for building)

## Installation

### From Release
1. Download the latest `mobclash-1.0.0.jar` from releases
2. Place it in your server's `plugins/` folder
3. Restart your server
4. Configuration files will be generated in `plugins/MobClash/`

### Building from Source
```bash
# Clone or download the project
cd mobclash

# Build with Maven
mvn clean package

# The JAR will be in target/mobclash-1.0.0.jar
cp target/mobclash-1.0.0.jar /path/to/server/plugins/
```

## Quick Start

### 1. Create a spawn group
```
/addspawn arena
/addspawn arena
/addspawn arena
```
Stand at each location where you want mobs to spawn.

### 2. Fill a chest with spawn eggs
Place a chest and fill it with the spawn eggs you want (e.g., 10 zombie eggs, 5 skeleton eggs).

### 3. Link the chest to your group
```
/setchest arena
```
Look at the chest when running this command.

### 4. Summon mobs!
```
/summonmobs arena random 5    # Spawn 5 mobs at one random point
/summonmobs arena all 1       # Spawn 1 mob at each point
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/addspawn <group>` | Add your current location to a spawn group | `mobspawner.addspawn` |
| `/removespawn <group>` | Remove the nearest spawn point from a group | `mobspawner.removespawn` |
| `/listgroups` | List all spawn groups and their point counts | `mobspawner.listgroups` |
| `/showspawns <group>` | Display spawn points with visual markers (3 seconds) | `mobspawner.showspawns` |
| `/setchest <group>` | Set the spawn egg chest for a group (look at chest) | `mobspawner.setchest` |
| `/summonmobs <group> <random\|all> [amount]` | Summon mobs from the group's chest | `mobspawner.summon` |

**Note:** All commands work from command blocks without requiring permissions!

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `mobspawner.*` | Grants all permissions | op |
| `mobspawner.addspawn` | Add spawn points | op |
| `mobspawner.removespawn` | Remove spawn points | op |
| `mobspawner.listgroups` | List spawn groups | op |
| `mobspawner.showspawns` | Show spawn markers | op |
| `mobspawner.setchest` | Set group chests | op |
| `mobspawner.summon` | Summon mobs | op |

## Configuration

### config.yml
```yaml
# Logging level for plugin operations
# Options: INFO (verbose), WARNING (quiet), SEVERE (errors only), OFF (silent)
logging-level: INFO
```

### language.yml
All messages are customizable! Edit `language.yml` to translate or customize messages:
```yaml
no-permission: "&cYou don't have permission to use this command."
addspawn-success: "&aAdded spawn point to group '{0}'! Total points: {1}"
summonmobs-success: "&aSpawned {0} mob(s) in group '{1}'!"
# ... and many more
```

Use `&` for color codes (e.g., `&a` = green, `&c` = red, `&e` = yellow).

## Examples

### Arena System
```bash
# Create arena spawn points around a battle area
/addspawn arena
# (move to different locations and repeat)

# Set chest with hostile mobs
/setchest arena

# Trigger wave with command block
/summonmobs arena all 3
```

### Boss Fight
```bash
# Create boss spawn point
/addspawn boss_room

# Set chest with boss mob egg (ender dragon, wither, etc.)
/setchest boss_room

# Spawn boss
/summonmobs boss_room random 1
```

### Random Ambushes
```bash
# Create multiple ambush points
/addspawn ambush
# (repeat at various locations)

# Set chest with surprise mobs
/setchest ambush

# Random spawn with redstone
/summonmobs ambush random 5
```

## Advanced Usage

### Command Blocks
All commands work perfectly in command blocks without permission issues:
```
Command Block: /summonmobs arena random 10
Redstone Signal → Mob spawn!
```

### Weighted Mob Spawning
The chest system is weighted by stack size:
- 10 zombie eggs + 2 creeper eggs = 83% zombies, 17% creepers
- Adjust egg amounts to control mob distribution

### Multiple Groups
Create different groups for different scenarios:
- `easy_wave` - weak mobs
- `hard_wave` - strong mobs  
- `boss_adds` - boss minions
- `ambient` - peaceful spawns

## Development

### Project Structure
```
mobclash/
├── pom.xml                          # Maven configuration
├── src/main/java/                   # Source code
│   ├── MobSpawnerPlugin.java       # Main plugin class
│   ├── commands/                    # Command implementations
│   └── managers/                    # Spawn & language managers
├── src/main/resources/              # Plugin resources
│   ├── plugin.yml                   # Plugin metadata
│   ├── config.yml                   # Default configuration
│   └── language.yml                 # Default messages
└── src/test/java/                   # Test suite
```

### Building
```bash
# Run tests only
mvn test

# Run integration tests
mvn integration-test

# Full build with tests
mvn clean verify
```

### Testing
The plugin includes comprehensive tests:
- **Unit Tests** - Test individual components (12+ tests)
- **Integration Tests** - Test complete workflows (8+ tests)
- Coverage includes permissions, command blocks, spawn logic, and more

## Troubleshooting

**Mobs not spawning?**
- Check that the group exists: `/listgroups`
- Verify chest is set: `/showspawns <group>` to see spawn points
- Ensure chest has spawn eggs
- Check console for error messages

**Permission denied?**
- Grant the appropriate `mobspawner.*` permission
- Or use command blocks which bypass permissions

**Configuration not loading?**
- Delete `plugins/MobClash/config.yml` to regenerate defaults
- Check console for YAML syntax errors
- Verify file encoding is UTF-8

## Support

For issues, questions, or contributions:
- Check the console logs (set `logging-level: INFO` for details)
- Review this README
- Submit issues with full error logs and configuration

## License

This plugin is provided as-is for use on Minecraft servers.

## Changelog

### v1.0.0 (Initial Release)
- Multiple spawn groups with configurable points
- Per-group chest system
- Random and all-location spawn modes
- Visual spawn markers
- Command block support
- Configurable logging
- Full test coverage
- Translatable messages

---

**Made with ❤️ for the Minecraft community**

---

Note from the author:
Vast majority of this was created quickly using Claude. Verification is in progress