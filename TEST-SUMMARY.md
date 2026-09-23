# MobClash Test Suite Summary

## Test Coverage Overview

**Total Test Files:** 12
**Total Test Cases:** 103 (95 unit, run by `mvn test`; 8 integration, run only by `mvn verify`)
**Package:** `io.tjs.mobclash`

> Counts here are maintained by hand and have drifted before. Treat the surefire/failsafe output
> as the source of truth, not this file.

---

## Unit Tests

### 1. SpawnManagerTest.java (11 tests)
**Location:** `src/test/java/io/tjs/mobclash/managers/`

✅ `testAddSpawnPoint` - Verify spawn points are added correctly
✅ `testAddMultipleSpawnPoints` - Multiple spawn points per group
✅ `testRemoveNearestSpawnPoint` - Remove closest spawn point to player
✅ `testSetGroupChest` - Set chest with wave parameter
✅ `testMultipleWavesPerGroup` - Multiple waves per group support
✅ `testGetAllGroups` - Retrieve all groups
✅ `testHasGroup` - Check group existence
✅ `testRemoveLastSpawnPointRemovesGroup` - Auto-cleanup empty groups
✅ `testGetRandomSpawnPoint` - Random spawn point selection
✅ `testGetGroupWavesEmptyForNonExistentGroup` - Handle missing groups
✅ `testGetGroupChestReturnsNullForNonExistentWave` - Handle missing waves
✅ Wave system integration

**Key Features Tested:**
- CRUD operations for spawn points
- Wave system (multiple chests per group)
- Config persistence
- Nearest point calculation
- Auto-cleanup

---

### 2. SpawnManagerPersistenceTest.java (6 tests)
**Location:** `src/test/java/io/tjs/mobclash/managers/`

Runs against a real `YamlConfiguration` and re-parses the serialized YAML, so the save/load path
is exercised rather than mocked. Covers the round trip, a group whose world is no longer loaded,
a dotted name, and the staged save that must not clear the tree before it can write it back.

---

### 3. LanguageManagerTest.java (7 tests)
**Location:** `src/test/java/io/tjs/mobclash/managers/`

✅ `testGetMessageSimple` - Basic message retrieval
✅ `testGetMessageWithPlaceholder` - Single placeholder replacement
✅ `testGetMessageWithMultiplePlaceholders` - Multiple placeholder replacement
✅ `testGetMessageMissingKey` - Missing translation handling
✅ `aKeyMissingFromAnOlderCopyFallsBackToTheBundledOne` - an upgraded server's old file still gets new keys
✅ `aLocallyEditedMessageWinsOverTheBundledOne` - the data-folder copy takes priority
✅ `testColorCodeConversion` - & to § color code conversion

**Key Features Tested:**
- Message loading from YAML
- Placeholder replacement ({0}, {1}, etc.)
- Color code conversion
- Missing key fallback

---

### 4. MobTrackerTest.java (11 tests)
**Location:** `src/test/java/io/tjs/mobclash/managers/`

✅ `testTagMob` - Tag mobs with NBT data
✅ `testIsMobClashMob` - Detect MobClash mobs
✅ `testIsNotMobClashMob` - Detect non-MobClash mobs
✅ `testGetMobSpawnInfo` - Retrieve spawn info from mob
✅ `testRecordKill` - Record single kill
✅ `testRecordMultipleKills` - Record multiple kills
✅ `testGetKillsByUUID` - Retrieve kills by UUID
✅ `testGetTopKills` - Leaderboard sorting
✅ `testResetKills` - Reset individual player kills
✅ `testResetAllKills` - Reset all player kills

**Key Features Tested:**
- NBT tagging system
- Kill tracking per player
- Leaderboard functionality
- Reset operations
- Persistent data handling

---

### 5. AddSpawnCommandTest.java (6 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ `testAddSpawnSuccessfully` - Add spawn point with permission
✅ `testAddSpawnNoPermission` - Permission denial
✅ `testAddSpawnMissingArguments` - Usage message on missing args
✅ `testCommandBlockBypassesPermissionAndUsesItsOwnLocation` - Bypass, at the block's position
✅ `testConsoleHasNoLocationToAdd` - Console has no position to add
✅ `testDottedGroupNameIsRejected` - A name that cannot round-trip through the config

**Key Features Tested:**
- Permission checks
- Command block bypass and its own location
- Argument and name validation

---

### 6. ListSpawnsCommandTest.java (5 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ `eachSpawnPointIsListedInOrderWithRoundedCoordinates` - Ordering, world, rounding
✅ `noArgumentsPrintsTheUsage` - Usage message validation
✅ `aMissingGroupIsReportedWithItsName` - Handle non-existent groups
✅ `anEmptyGroupIsReportedWithItsName` - Handle empty groups
✅ `aSenderWithoutThePermissionIsRefused` - Permission denial

**Key Features Tested:**
- Coordinate listing, ordering, and rounding of negatives
- Group validation and empty state handling

---

### 7. SummonMobsCommandTest.java (18 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ `randomModeSpreadsTheRequestedCountOverThePoints` - Random spawn mode
✅ `allModeSpawnsTheRequestedCountAtEveryPoint` - All locations spawn mode
✅ `aCommandBlockMaySummonWithoutPermission` - Command block bypass
✅ `anEggWhoseEnumNameDiffersFromItsEntityStillMaps` - MOOSHROOM egg -> MUSHROOM_COW
✅ `nonEggContentsAndEmptySlotsAreIgnored` - Mixed chest contents
✅ `aRefusedSpawnIsNotCountedOrTagged` - Cancelled CreatureSpawnEvent
✅ `tooFewArgumentsPrintsTheUsage` - Argument validation
✅ `anUnknownModeIsRejectedBeforeAnythingIsLookedUp` - Mode validation and its ordering
✅ `aMissingGroupIsReported` / `aWaveWithNoChestIsReported` / `aGroupWithNoSpawnPointsIsReported`
✅ `anAmountAboveTheParseBoundIsRejected` / `anAmountBelowOneIsRejected` / `aNonNumericAmountIsRejected`
✅ `aSummonOverTheTotalCapIsRefused` - amount x points against max-mobs-per-summon
✅ `aChestThatIsNoLongerThereIsReported` / `aChestWithNoSpawnEggsIsReported`
✅ `aPlayerWithoutThePermissionIsRefused` - Permission denial

**Key Features Tested:**
- Wave system integration, random vs all modes, mob tagging
- Spawn-egg to entity mapping through namespaced keys, not enum names
- Every guard clause, each asserted by the message key it sends

---

### 8. PluginYmlTest.java (7 tests)
**Location:** `src/test/java/io/tjs/mobclash/`

✅ `theDescriptorParsesAndNamesThePlugin` - plugin.yml is valid YAML naming the main class
✅ `theVersionPlaceholderIsSubstitutedByResourceFiltering` - `${project.version}` is filtered
✅ `everyPermissionNodeUsedInCodeIsDeclared` - no node exists only in Java
✅ `theWildcardGrantsEveryNodeUsedInCode` - `mobclash.*` reaches every node
✅ `theDeprecatedNamespaceStillGrantsTheNewNodes` - `mobspawner.*` still works
✅ `everyCommandHelpEntryIsUsableAsBukkitRendersIt` - /help has a description and a `/<command>` usage
✅ `everyDeclaredCommandIsRegisteredByThePlugin` - no command declared without an executor

**Key Features Tested:**
- The descriptor, which nothing else compiles or checks

---

### 9. KillBoardTest.java (10 tests)
**Location:** `src/test/java/io/tjs/mobclash/managers/`

Each fake scoreboard is backed by a map of its sidebar lines, so the tests assert on what a
player would read.

✅ `showingPutsTheLeaderboardInTheSidebar`
✅ `aViewerOutsideTheTopTenStillSeesTheirOwnLine`
✅ `aNewViewerWithNoKillsSeesAZero`
✅ `refreshUpdatesCountsAndDropsLinesThatLeft`
✅ `hidingPutsBackTheScoreboardThePlayerHadBefore`
✅ `hidingLeavesAScoreboardAnotherPluginSwappedIn`
✅ `toggleFlipsAndReportsTheNewState`
✅ `showingTwiceKeepsOneBoard`
✅ `hideAllRestoresEveryViewerAndCountsThem`
✅ `aPlayerWhoQuitsComesBackWithItOff`

---

### 10. KillBoardCommandTest.java (9 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ `aPlayerTogglesTheirOwn`
✅ `theConsoleHasNoBoardOfItsOwn`
✅ `worldOnShowsItToEveryoneInTheSendersWorld`
✅ `worldOffHidesItFromEveryoneInTheSendersWorld`
✅ `aCommandBlockUsesItsOwnWorld`
✅ `theConsoleHasNoWorldToSwitch`
✅ `theAdminFormsNeedTheAdminPermission`
✅ `allOffTurnsItOffEverywhere`
✅ `aBadStateOrSubcommandPrintsTheUsage`

---

## Integration Tests

### 11. DataFileTest.java (5 tests)
**Location:** `src/test/java/io/tjs/mobclash/`

✅ `adoptMovesTheWholeTreeAndLeavesNothingBehind` - the one-time config.yml migration
✅ `adoptLeavesOperatorSettingsAlone` - settings do not follow the data across
✅ `adoptReportsThatAFreshInstallHasNothingToMove` - no migration on a new server
✅ `adoptedDataSurvivesTheWriteToDisk` - migrated data round-trips through YAML
✅ `aFileThatDoesNotExistYetOpensEmptyRatherThanFailing` - first-run behaviour

**Key Features Tested:**
- The upgrade path, which runs once and has no second chance to be right

---

### 12. MobClashPluginIT.java (8 tests)
**Location:** `src/test/java/io/tjs/mobclash/`

✅ `testCompleteWorkflowWithWaves` - End-to-end wave system
✅ `testMultipleGroupsAndWaves` - Multiple groups with multiple waves
✅ `testRemoveSpawnPoint` - Spawn point removal workflow
✅ `testKillTracking` - Kill recording and retrieval
✅ `testKillLeaderboard` - Leaderboard sorting and limits
✅ `testResetKills` - Individual kill reset
✅ `testResetAllKills` - Global kill reset
✅ `testMobTagging` - NBT tagging integration

**Key Features Tested:**
- Complete user workflows
- Manager integration
- Multi-wave systems
- Kill tracking system
- Data persistence

---

## Test Execution

### Run All Tests
```bash
mvn clean test
```

### Run Specific Test Class
```bash
mvn test -Dtest=SpawnManagerTest
mvn test -Dtest=MobTrackerTest
mvn test -Dtest=MobClashPluginIT
```

### Run Integration Tests Only
```bash
mvn verify
```

### Run with Coverage
```bash
mvn clean test jacoco:report
```

---

## Coverage Summary

| Component | Tests | Coverage |
|-----------|-------|----------|
| SpawnManager (behaviour) | 11 | ✅ High |
| SpawnManager (persistence round trip) | 6 | ✅ High |
| LanguageManager | 5 | ✅ High |
| MobTracker | 10 | ✅ High |
| Commands | 17 | ⚠️ Partial — `KillsCommand` has no tests |
| Listeners | 0 | ❌ None — `MobDeathListener` is untested |
| Integration | 8 | ⚠️ Manager-level only; no command, listener or lifecycle coverage |

---

## Key Testing Patterns

### Mocking
- Mocks are strict. A stub a test never reaches fails that test, which is how an untested branch
  announces itself; blanket `@Mock(lenient = true)` used to suppress exactly that signal.
- Only shared-fixture stubs are marked `lenient()`, individually and with the reason: a setUp
  stub that genuinely serves some tests and not others, such as the sender name every command
  logs on the way in. Anything stubbed inside a test is strict.
- Command tests stub `getMessage` to echo its key back rather than a fixed string, so an
  assertion names the branch that fired. Two guards swapped by mistake fail instead of pass.
- `SpawnManagerPersistenceTest` deliberately avoids mocking the config: it runs against a real
  `YamlConfiguration` and re-parses the serialized YAML, so the save/load path is genuinely
  exercised.

### Test Structure
```java
@BeforeEach
void setUp() {
    // Mock setup
    // Stub common methods
    // Create test objects
}

@Test
void testFeatureName() {
    // Arrange
    // Act
    // Assert
    // Verify
}
```

### Assertions
- JUnit 5 assertions (`assertEquals`, `assertTrue`, etc.)
- Mockito verifications (`verify`, `times`, `never`)
- State verification after operations

---

## New Features Tested

### Wave System ⭐
- Multiple chests per group
- Wave-specific spawning
- Wave configuration persistence

### Kill Tracking ⭐
- Player kill counting
- Leaderboard functionality
- Reset operations
- UUID-based tracking

### Mob Tagging ⭐
- NBT data persistence
- Spawn info retrieval
- MobClash vs natural mob distinction

### Loot to Inventory ❌
- **Not tested.** `MobDeathListener` has no test of any kind, and no test sets
  `loot-to-inventory`. This was previously listed here as covered; it was not.

---

## Test Files Checklist

- [x] SpawnManagerTest.java
- [x] SpawnManagerPersistenceTest.java
- [x] LanguageManagerTest.java
- [x] MobTrackerTest.java
- [x] AddSpawnCommandTest.java
- [x] ListSpawnsCommandTest.java
- [x] SummonMobsCommandTest.java
- [x] PluginYmlTest.java
- [x] KillBoardTest.java
- [x] KillBoardCommandTest.java
- [x] DataFileTest.java
- [x] MobClashPluginIT.java
- [ ] KillsCommandTest.java (can be added)
- [ ] MobDeathListenerTest.java (can be added)

---

## Running Tests Successfully

Unit tests only:
```bash
mvn clean test
```

Unit **and** integration tests — `MobClashPluginIT` is bound to failsafe, so `mvn test` never
runs it:
```bash
mvn clean verify
```

---

## Notes

- All tests updated to use `io.tjs.mobclash` package
- Wave system fully integrated in all relevant tests
- MobTracker tests verify NBT tagging
- Integration tests exercise the managers directly; they do not construct commands, register
  listeners, or run the plugin lifecycle
- `AddSpawnCommandTest` verifies that a command block bypasses the permission check and adds a
  point at its own position
- All managers properly mocked with logging support