# MobClash Test Suite Summary

## Test Coverage Overview

**Total Test Files:** 24
**Total Test Cases:** 227 (219 unit, run by `mvn test`; 8 integration, run only by `mvn verify`)
**Package:** `io.tjs.mobclash`

> Counts here are maintained by hand and have drifted before. Treat the surefire/failsafe output
> as the source of truth, not this file.

---

## Unit Tests

### 1. SpawnManagerTest.java (20 tests)
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
✅ `removeSpawnPointRemovesThePointAtThatIndexAndSavesTheRest` - `/removespawn <group> <number>`, checked in the saved `spawns.yml`
✅ `removeSpawnPointOutOfRangeChangesNothing` - past the end, negative, unknown group
✅ `removingTheLastPointByIndexRemovesTheGroup` - same empty-group cleanup as the nearest form
✅ `removeGroupChestForgetsOnlyThatChest` - the other chest stays, on disk too
✅ `removingTheLastChestDropsTheGroupFromTheChestGroups` - no empty chest group left behind
✅ `removeGroupChestThatIsNotSetChangesNothing` - unknown chest or group
✅ `removeGroupForgetsItsPointsAndChestsAndKeepsOtherGroups` - `/removegroup ... confirm`
✅ `removeGroupRemovesAGroupThatHasOnlyChests` - a group made with `/setchest` alone
✅ `removeGroupOfAnUnknownGroupReturnsFalse`
✅ Wave system integration

**Key Features Tested:**
- CRUD operations for spawn points, chests and whole groups
- Wave system (multiple chests per group)
- Config persistence, read back from the written `spawns.yml`
- Nearest point calculation
- Auto-cleanup

---

### 2. SpawnManagerPersistenceTest.java (6 tests)
**Location:** `src/test/java/io/tjs/mobclash/managers/`

Runs against a real `YamlConfiguration` and re-parses the serialized YAML, so the save/load path
is exercised rather than mocked. Covers the round trip, a group whose world is no longer loaded,
a dotted name, and the staged save that must not clear the tree before it can write it back.

---

### 3. LanguageManagerTest.java (10 tests)
**Location:** `src/test/java/io/tjs/mobclash/managers/`

✅ `testGetMessageSimple` - Basic message retrieval
✅ `testGetMessageWithPlaceholder` - Single placeholder replacement
✅ `testGetMessageWithMultiplePlaceholders` - Multiple placeholder replacement
✅ `testGetMessageMissingKey` - Missing translation handling
✅ `aKeyMissingFromAnOlderCopyFallsBackToTheBundledOne` - an upgraded server's old file still gets new keys
✅ `anUnversionedCopyLosesTheUsageTextThatMovedToPluginYml` - the format 0 to 1 step, including the old `chest-missing` and `setchest-success` lines
✅ `anUpgradedCopyNamesTheMissingChestWithTheBundledText` - the new `chest-missing` text and its placeholders
✅ `aLocallyEditedMessageWinsOverTheBundledOne` - the data-folder copy takes priority
✅ `reloadPicksUpAnEditMadeOnDisk` - `/mobclash reload` rereads the file
✅ `testColorCodeConversion` - & to § color code conversion

**Key Features Tested:**
- Message loading from YAML
- Placeholder replacement ({0}, {1}, etc.)
- Color code conversion
- Missing key fallback

---

### 4. MobTrackerTest.java (14 tests)
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
✅ `removeMobsTakesOnlyTaggedMobsInEveryWorld` - `/killmobs`; untagged mobs and players stay
✅ `removeMobsWithAGroupTakesOnlyThatGroupNotOneWhoseNameStartsTheSame` - group `a` leaves `ab:wave1`
✅ `removeMobsWithAnUnknownGroupRemovesNothing`

**Key Features Tested:**
- NBT tagging system
- Kill tracking per player
- Leaderboard functionality
- Reset operations
- Persistent data handling
- `/killmobs` removal by tag and group

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

### 6. ListSpawnsCommandTest.java (6 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ `eachSpawnPointIsListedInOrderWithRoundedCoordinates` - Ordering, world, rounding
✅ `aPointWhoseWorldWasUnloadedIsListedAsSuch` - no throw when the world is gone
✅ `noArgumentsPrintsTheUsage` - Usage message validation
✅ `aMissingGroupIsReportedWithItsName` - Handle non-existent groups
✅ `anEmptyGroupIsReportedWithItsName` - Handle empty groups
✅ `aSenderWithoutThePermissionIsRefused` - Permission denial

**Key Features Tested:**
- Coordinate listing, ordering, and rounding of negatives
- Group validation and empty state handling

---

### 7. SummonMobsCommandTest.java (29 tests)
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
✅ `anAmountAboveTheParseBoundIsRejected` / `anAmountBelowOneIsRejected`
✅ `anArgumentThatIsNeitherAnAmountNorEquipmentIsRejected` / `anExtraArgumentPrintsTheUsage`
✅ `anEggsEntityDataIsKeptOnTheSpawnedMob` / `gearFromAnEggNeverDrops` / `aRefusedSpawnFromADataEggIsNotTagged`
✅ `aDataEggThatIsNotAMobIsSkippedWithAWarning` / `aDataEggWithRidersIsSkippedWithAWarning` / `aDataEggWithAFixedUuidIsSkippedWithAWarningToTheCommandBlock`
✅ `equipmentFalseLeavesVanillaGearAlone` - no equipment argument, no change to vanilla gear
✅ `equippingAPlainEggMobClearsVanillaGearAndNothingDrops` - via an empty gear chest
✅ `aGearChestThatIsNoLongerThereIsReported` - names the group, chest, block found, world and position
✅ `aSummonOverTheTotalCapIsRefused` - amount x points against max-mobs-per-summon
✅ `aChestThatIsNoLongerThereIsReported` - the same detail for the wave chest / `aChestWithNoSpawnEggsIsReported`
✅ `aPlayerWithoutThePermissionIsRefused` - Permission denial

**Key Features Tested:**
- Wave system integration, random vs all modes, mob tagging
- Spawn-egg to entity mapping through namespaced keys, not enum names
- Every guard clause, each asserted by the message key it sends
- Egg entity data kept, bad data eggs skipped with a warning
- Equipment argument parsing and the clear-then-equip wiring. What is drawn is tested in
  RandomEquipmentTest; the equip step needs a server registry and was tested live

---

### 8. PluginYmlTest.java (7 tests)
**Location:** `src/test/java/io/tjs/mobclash/`

✅ `theDescriptorParsesAndNamesThePlugin` - plugin.yml is valid YAML naming the main class
✅ `theVersionPlaceholderIsSubstitutedByResourceFiltering` - `${project.version}` is filtered
✅ `everyPermissionNodeUsedInCodeIsDeclared` - no node exists only in Java
✅ `theWildcardGrantsEveryNodeUsedInCode` - `mobclash.*` reaches every node
✅ `everyCommandHelpEntryIsUsableAsBukkitRendersIt` - /help has a description and a `/<command>` usage
✅ `everyDeclaredCommandIsRegisteredByThePlugin` - no command declared without an executor
✅ `everyCommandHasALineInMobclashHelp` - `/mobclash help` never prints a missing translation

**Key Features Tested:**
- The descriptor, which nothing else compiles or checks

---

### 9. KillBoardTest.java (20 tests)
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

### 10. KillBoardCommandTest.java (17 tests)
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

### 11. RandomEquipmentTest.java (11 tests)
**Location:** `src/test/java/io/tjs/mobclash/`

The draw is generic over the item type, so these run on strings and Materials, 200,000 draws
each with a fixed seed.

✅ `zombiesGetArmorAndWeaponsAtVanillasHardDifficultyRates` - 15% armor, 5% weapons, tier and piece odds
✅ `aPieceIsNeverWornWithoutThePiecesBeforeIt` - boots first, one tier per mob
✅ `skeletonsNeverDrawAWeapon`
✅ `aOneTierLadderTakesEveryArmoredMob` - a gear chest's single tier
✅ `enchantmentLevelsStayWithinTheConfiguredMaximum` / `aMaximumOfZeroTurnsEnchantingOff`
✅ `emptyPoolsGiveNothing`
✅ `theShippedDefaultsAreTheRequestedGear` - the jar's config.yml lists
✅ `aConfigFromBeforeTheSettingGetsTheShippedDefaults` / `aSettingLeftOutOfTheServersSectionFallsBackToItsDefault`
✅ `unknownConfigEntriesAreSkippedWithAWarning`

---

### 12. MobClashCommandTest.java (6 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ `helpListsOnlyTheCommandsTheSenderMayRun` - `/mobclash`, `/mobclash help`
✅ `aSubcommandRunsWithTheArgumentsAfterItsName` - `/mobclash kills top 5`, any case
✅ `aSubcommandKeepsItsOwnPermission`
✅ `anUnknownSubcommandIsNamedWithAPointerToHelp`
✅ `tabCompletionOffersHelpAndTheUsableSubcommandsByPrefix`

---

### 13. ReloadCommandTest.java (3 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ `reloadsThenConfirmsInTheReloadedLanguage`
✅ `aFileThatDoesNotParseIsNamedAndNotReportedAsReloaded`
✅ `aSenderWithoutThePermissionIsRefused`

---

### 14. RequireParsesTest.java (3 tests)
**Location:** `src/test/java/io/tjs/mobclash/`

✅ `validYamlPasses` / `brokenYamlIsRejectedByName` / `aMissingFilePasses`

---

### 15. MobDeathListenerTest.java (10 tests)
**Location:** `src/test/java/io/tjs/mobclash/listeners/`

✅ `lootGoesToTheKillerAndOnlyWhatDoesNotFitStaysOnTheGround`
✅ `withTheSettingOffTheDropsAreLeftAlone`
✅ `aKillerWhoLoggedOutLeavesTheLootOnTheGroundButTheKillCounts`
✅ `aKillerWhoDiedLeavesTheLootOnTheGroundButTheKillCounts`
✅ `aMobNotSpawnedByMobClashIsIgnored`
✅ `aDeathNotCausedByAPlayerIsNotCountedOrLooted`
✅ `aListedMobsDeathIsAnnouncedToTheWholeServer` - with or without `minecraft:`, any case
✅ `aMobMissingFromTheListIsNotAnnounced`
✅ `aListedMobMobClashDidNotSummonIsNotAnnounced`
✅ `theHandlerSkipsCancelledDeathsAndRunsAfterOtherPlugins`

---

### 15a. ListChestsCommandTest.java (9 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ A present chest with its egg and item counts, a missing chest naming the block found, a chest whose world was unloaded, the group
filter, an unknown group, no chests at all, sort order, extra arguments, permission refusal

### 15b. VersionCommandTest.java (2 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ Replies with the plugin name, version and server version; permission refusal

### 15c. RemoveSpawnCommandTest.java (10 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ Without a number, the point nearest the player; the console has no position for that form.
A number removes that point from the console, and from a command block with the reply logged.
A point whose world was unloaded is still removed and the reply says so. A number past the end,
zero or a word changes nothing and names the range. Unknown group, too many arguments.

### 15d. RemoveChestCommandTest.java (5 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ A set chest is forgotten; a chest that is not set is reported; a command block runs it and its
reply reaches the console; wrong argument count prints the usage; permission refusal

### 15e. RemoveGroupCommandTest.java (7 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ Without `confirm`, the counts it would remove and nothing removed. With `confirm`, the group
removed and the counts reported, case-insensitive. A chest-only group counts as existing. An
unknown group, a second argument other than `confirm`, permission refusal.

### 15f. KillMobsCommandTest.java (4 tests)
**Location:** `src/test/java/io/tjs/mobclash/commands/`

✅ Without a group, the count removed is reported; with one, the group and its count; an extra
argument prints the usage and removes nothing; permission refusal. Which mobs go is covered in
MobTrackerTest.

## Integration Tests

### 16. DataFileTest.java (7 tests)
**Location:** `src/test/java/io/tjs/mobclash/`

✅ `adoptMovesTheWholeTreeAndLeavesNothingBehind` - the one-time config.yml migration
✅ `adoptLeavesOperatorSettingsAlone` - settings do not follow the data across
✅ `adoptReportsThatAFreshInstallHasNothingToMove` - no migration on a new server
✅ `adoptedDataSurvivesTheWriteToDisk` - migrated data round-trips through YAML
✅ `aFileThatDoesNotExistYetOpensWithOnlyItsFormatRatherThanFailing` - first-run behaviour
✅ `anUnversionedFileIsStampedOnTheNextSave` - a pre-2.2.0 file gets `format-version`
✅ `aNewFileStartsAtTheCurrentFormat` - a new file is written already stamped

**Key Features Tested:**
- The upgrade path, which runs once and has no second chance to be right

---

### 16a. FileFormatTest.java (7 tests)
**Location:** `src/test/java/io/tjs/mobclash/`

✅ `anUnversionedFileRunsEveryStepAndIsStamped` - a missing `format-version` counts as 0
✅ `onlyTheStepsAfterTheFilesVersionRun` - steps already applied are skipped
✅ `aCurrentFileIsLeftAlone` - no change, no save, no log line
✅ `aFileFromANewerReleaseIsLeftAloneWithAWarning` - a downgrade does not touch the file
✅ `theBundledDefaultsDoNotCountAsTheFilesVersion` - the jar's copy cannot stand in for the file's
✅ `aRemovedKeysCommentsMoveToTheNextKey` - a section heading survives its first key's removal
✅ `theStampExplainsItselfInTheFile` - the new `format-version` line carries its comment

---

### 17. MobClashPluginIT.java (8 tests)
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
| SpawnManager (behaviour) | 20 | ✅ High |
| SpawnManager (persistence round trip) | 6 | ✅ High |
| LanguageManager | 10 | ✅ High |
| MobTracker | 14 | ✅ High |
| RandomEquipment (draw and config) | 11 | ✅ High; the equip step was tested live |
| Commands | 104 | ⚠️ Partial — `KillsCommand` has no tests |
| Listeners | 10 | ✅ `MobDeathListener` |
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
- Server-wide announcements for listed mobs, MobClash mobs only

### Commands under /mobclash ✅
- `MobClashCommandTest`: help lists only what the sender may run, arguments pass through to the
  subcommand, each subcommand keeps its permission, unknown names point to help, tab completion
- `PluginYmlTest`: every command has a help line in `language.yml`

### Mob Tagging ⭐
- NBT data persistence
- Spawn info retrieval
- MobClash vs natural mob distinction

### Loot to Inventory ✅
- `MobDeathListenerTest`: drops move to the killer and only what does not fit stays on the
  ground; a killer who has logged out or died gets nothing and the drops stay, but the kill
  counts; the setting off leaves drops alone; non-MobClash mobs and non-player kills are skipped
  with their drops untouched;
  the handler is registered to skip cancelled deaths (Bukkit enforces that, so only the flags are
  pinned). The loot table itself runs in the server, not here; smoke-tested live instead.

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
- [x] FileFormatTest.java
- [x] MobClashPluginIT.java
- [x] RandomEquipmentTest.java
- [x] MobClashCommandTest.java
- [x] ReloadCommandTest.java
- [x] RequireParsesTest.java
- [x] MobDeathListenerTest.java
- [x] ListChestsCommandTest.java
- [x] VersionCommandTest.java
- [x] RemoveSpawnCommandTest.java
- [x] RemoveChestCommandTest.java
- [x] RemoveGroupCommandTest.java
- [x] KillMobsCommandTest.java
- [ ] KillsCommandTest.java (can be added)

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