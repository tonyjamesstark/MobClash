# MobClash

A powerful Minecraft Paper plugin for managing configurable mob spawn groups with per-group customization and flexible spawning modes.

⚔️ **Clash with endless possibilities!**

## Features

- 🎯 **Multiple Spawn Groups** - Create unlimited spawn groups, each with multiple spawn points
- 🌊 **Wave System** - Multiple chests per group for different mob waves and difficulty progression
- 📦 **Per-Wave Mob Pools** - Each wave has its own chest of spawn eggs for complete control
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
1. Download the latest `mobclash-1.1.0.jar` from releases
2. Place it in your server's `plugins/` folder
3. Restart your server
4. Configuration files will be generated in `plugins/MobClash/`

### Building from Source
```bash
# Clone or download the project
cd mobclash

# Check code formatting
mvn spotless:check

# Auto-fix formatting issues
mvn spotless:apply

# Build with Maven (runs formatting check automatically)
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

### 2. Fill chests with spawn eggs for different waves
Create multiple chests with different mob compositions:
- **Chest 1** (easy wave): 10 zombie eggs, 5 skeleton eggs
- **Chest 2** (hard wave): 5 zombie eggs, 5 skeleton eggs, 5 creeper eggs
- **Chest 3** (boss wave): 1 wither egg

### 3. Link each chest to your group as a different wave
```
/setchest arena wave1    # Look at chest 1
/setchest arena wave2    # Look at chest 2
/setchest arena boss     # Look at chest 3
```

### 4. Summon specific waves!
```
/summonmobs arena wave1 random 5    # Spawn 5 easy mobs at one random point
/summonmobs arena wave2 all 3       # Spawn 3 hard mobs at each point
/summonmobs arena boss random 1     # Spawn 1 boss mob
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/addspawn <group>` | Add your current location to a spawn group | `mobspawner.addspawn` |
| `/removespawn <group>` | Remove the nearest spawn point from a group | `mobspawner.removespawn` |
| `/listgroups` | List all spawn groups and their point counts | `mobspawner.listgroups` |
| `/listspawns <group>` | List all spawn point coordinates for a group | `mobspawner.listspawns` |
| `/showspawns <group>` | Display spawn points with visual markers (3 seconds) | `mobspawner.showspawns` |
| `/setchest <group> <wave>` | Set the spawn egg chest for a group wave (look at chest) | `mobspawner.setchest` |
| `/summonmobs <group> <wave> <random\|all> [amount]` | Summon mobs from the specified wave | `mobspawner.summon` |
| `/kills [top\|reset\|resetall] [amount]` | View kill statistics and leaderboard | `mobspawner.kills` |

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

### Progressive Wave System
```bash
# Create arena spawn points around a battle area
/addspawn arena
# (move to different locations and repeat)

# Set up wave progression
/setchest arena wave1    # Chest with: 10 zombies
/setchest arena wave2    # Chest with: 5 zombies, 5 skeletons
/setchest arena wave3    # Chest with: 3 zombies, 3 skeletons, 4 creepers
/setchest arena boss     # Chest with: 1 wither or ender dragon

# Trigger waves with command blocks
/summonmobs arena wave1 all 3
# ... wait 60 seconds ...
/summonmobs arena wave2 all 3
# ... wait 60 seconds ...
/summonmobs arena wave3 all 3
# ... wait 60 seconds ...
/summonmobs arena boss random 1
```

### Themed Mob Waves
```bash
# Undead wave
/setchest dungeon undead    # Chest: zombies, skeletons, zombie pigmen

# Ranged wave  
/setchest dungeon ranged    # Chest: skeletons, strays, pillagers

# Explosive wave
/setchest dungeon explosive # Chest: creepers, TNT minecarts

# Flying wave
/setchest dungeon flying    # Chest: phantoms, vexes, blazes

# Trigger specific themed waves
/summonmobs dungeon undead all 5
/summonmobs dungeon explosive random 3
```

### Boss Fight with Minion Waves
```bash
# Create boss room spawn points
/addspawn boss_room    # Center for boss
/addspawn boss_room    # Corners for adds
/addspawn boss_room
/addspawn boss_room

# Set up different waves
/setchest boss_room boss     # Chest: 1 wither
/setchest boss_room adds     # Chest: 5 zombies, 5 skeletons

# Boss fight sequence
/summonmobs boss_room boss random 1      # Spawn boss in center
# ... every 30 seconds ...
/summonmobs boss_room adds all 2         # Spawn adds at all corners
```

### Random Ambushes
```bash
# Create multiple ambush points around your base
/addspawn ambush
# (repeat at various locations)

# Different difficulty tiers
/setchest ambush easy     # Chest: 10 zombies, 5 spiders
/setchest ambush hard     # Chest: 5 creepers, 3 endermen, 2 blazes

# Random spawn with redstone triggers
/summonmobs ambush easy random 5
/summonmobs ambush hard random 3
```

### Tower Defense Style
```bash
# Create spawn points along a path
/addspawn path1
/addspawn path2  
/addspawn path3

# Create difficulty progression
/setchest path1 round1   # 10 zombies
/setchest path1 round5   # 5 zombies, 5 skeletons
/setchest path1 round10  # 3 zombies, 3 skeletons, 4 creepers, 2 spiders
/setchest path1 round15  # Boss round!

# Spawn rounds sequentially
/summonmobs path1 round1 random 5
/summonmobs path1 round5 random 5
/summonmobs path1 round10 random 8
/summonmobs path1 round15 random 1
```

## Advanced Usage

### Command Blocks
All commands work perfectly in command blocks without permission issues:
```
Command Block 1: /summonmobs arena wave1 random 10
[Wait 60s]
Command Block 2: /summonmobs arena wave2 random 10
[Wait 60s]
Command Block 3: /summonmobs arena boss random 1
Redstone Chain → Progressive waves!
```

### Weighted Mob Spawning
The chest system is weighted by stack size:
- 10 zombie eggs + 2 creeper eggs = 83% zombies, 17% creepers
- Adjust egg amounts to control mob distribution within each wave

### Multiple Waves per Group
Create different waves for different scenarios:
- `arena/wave1` - Easy mobs (zombies)
- `arena/wave2` - Medium mobs (zombies + skeletons)
- `arena/wave3` - Hard mobs (zombies + skeletons + creepers)
- `arena/boss` - Boss mobs (wither, ender dragon)
- `arena/special` - Event mobs (special occasions)

Each wave uses the same spawn points but different mob compositions!

## Development

## Updated Checklist 📋

### Main Files:
- `MobClashPlugin.java` (main class)
- `SpawnManager.java` 
- `LanguageManager.java`
- All command files use `com.example.mobclash.commands`
- All manager files use `com.example.mobclash.managers`

### Directory Structure:
```
mobclash/
├── src/main/java/com/example/mobclash/
│   ├── MobClashPlugin.java
│   ├── commands/
│   └── managers/
└── src/test/java/com/example/mobclash/
```

### Building
```bash
# Check code formatting
mvn spotless:check

# Auto-fix code formatting
mvn spotless:apply

# Run tests only
mvn test

# Run integration tests
mvn integration-test

# Full build with tests and formatting
mvn clean verify
```

### Code Style
The project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format to enforce consistent code style:
- **Google Java Format** for Java files
- **Prettier** for YAML files  
- **Automatic license headers** on all source files
- **Import optimization** (removes unused imports)

Code formatting is automatically checked during the build. If formatting issues are found, the build will fail with instructions on how to fix them.

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

the vast majority of this was created as a test case for exploring AI coding workflows. Results may vary. Verification of the project is in progress....
