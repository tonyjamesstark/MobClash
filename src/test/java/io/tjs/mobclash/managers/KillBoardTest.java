package io.tjs.mobclash.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Each scoreboard the manager hands out is backed by a plain map of its sidebar lines, so the tests
 * assert on what a player would read rather than on which calls produced it. Kill counts come from
 * stubbing {@code MobTracker.getKills(Player)} directly rather than {@code getTopKills}, since
 * ranking is now computed per world from whoever is currently in it.
 */
@ExtendWith(MockitoExtension.class)
class KillBoardTest {

  @Mock private MobClashPlugin plugin;
  @Mock private MobTracker mobTracker;
  @Mock private LanguageManager langManager;
  @Mock private ScoreboardManager scoreboards;
  @Mock private Scoreboard mainBoard;

  private final Map<Scoreboard, Map<String, Integer>> sidebars = new HashMap<>();
  private KillBoard killBoard;

  @BeforeEach
  void setUp() {
    killBoard = new KillBoard(plugin, mobTracker, langManager, scoreboards);
    lenient().when(langManager.getMessage("killboard-title")).thenReturn("Kills");
    lenient().when(scoreboards.getNewScoreboard()).thenAnswer(invocation -> newBoard());
  }

  private Scoreboard newBoard() {
    Scoreboard board = mock(Scoreboard.class);
    Objective objective = mock(Objective.class);
    Map<String, Integer> lines = new HashMap<>();
    sidebars.put(board, lines);
    when(board.registerNewObjective(KillBoard.OBJECTIVE, "dummy", "Kills")).thenReturn(objective);
    lenient().when(board.getObjective(KillBoard.OBJECTIVE)).thenReturn(objective);
    lenient().when(board.getEntries()).thenAnswer(invocation -> Set.copyOf(lines.keySet()));
    lenient()
        .doAnswer(invocation -> lines.remove(invocation.<String>getArgument(0)))
        .when(board)
        .resetScores(anyString());
    lenient()
        .when(objective.getScore(anyString()))
        .thenAnswer(
            invocation -> {
              String name = invocation.getArgument(0);
              Score score = mock(Score.class);
              doAnswer(set -> lines.put(name, set.<Integer>getArgument(0)))
                  .when(score)
                  .setScore(anyInt());
              return score;
            });
    return board;
  }

  private World world() {
    World world = mock(World.class);
    lenient().when(world.getUID()).thenReturn(UUID.randomUUID());
    lenient().when(world.getPlayers()).thenReturn(List.of());
    return world;
  }

  /** Sets who World#getPlayers() reports for this world, and points each one back at it. */
  private void inWorld(World world, Player... players) {
    lenient().when(world.getPlayers()).thenReturn(List.of(players));
    for (Player player : players) {
      lenient().when(player.getWorld()).thenReturn(world);
    }
  }

  /** A player whose scoreboard behaves like the real one: set replaces what get returns. */
  private Player player(String name) {
    Player player = mock(Player.class);
    AtomicReference<Scoreboard> current = new AtomicReference<>(mainBoard);
    lenient().when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    lenient().when(player.getName()).thenReturn(name);
    lenient().when(player.getScoreboard()).thenAnswer(invocation -> current.get());
    lenient()
        .doAnswer(invocation -> current.getAndSet(invocation.getArgument(0)))
        .when(player)
        .setScoreboard(any());
    return player;
  }

  private void kills(Player player, int count) {
    lenient().when(mobTracker.getKills(player)).thenReturn(count);
  }

  private Map<String, Integer> sidebarOf(Player player) {
    return sidebars.get(player.getScoreboard());
  }

  @Test
  void showingPutsTheLeaderboardInTheSidebar() {
    World world = world();
    Player alice = player("Alice");
    Player bob = player("Bob");
    inWorld(world, alice, bob);
    kills(alice, 7);
    kills(bob, 3);

    assertTrue(killBoard.show(alice));

    assertNotSame(mainBoard, alice.getScoreboard());
    assertEquals(Map.of("Alice", 7, "Bob", 3), sidebarOf(alice));
    Objective objective = alice.getScoreboard().getObjective(KillBoard.OBJECTIVE);
    verify(objective).setDisplaySlot(DisplaySlot.SIDEBAR);
  }

  @Test
  void playersWithNoKillsStillAppearOnTheBoard() {
    World world = world();
    Player alice = player("Alice");
    Player dave = player("Dave");
    inWorld(world, alice, dave);
    kills(alice, 7);

    killBoard.show(dave);

    assertEquals(Map.of("Alice", 7, "Dave", 0), sidebarOf(dave));
  }

  @Test
  void playersInAnotherWorldDoNotAppear() {
    World arena = world();
    World nether = world();
    Player alice = player("Alice");
    Player eve = player("Eve");
    inWorld(arena, alice);
    inWorld(nether, eve);
    kills(eve, 50);

    killBoard.show(alice);

    assertEquals(Map.of("Alice", 0), sidebarOf(alice));
  }

  @Test
  void aViewerOutsideTheTopTenStillSeesTheirOwnLine() {
    World world = world();
    Player[] tops = new Player[KillBoard.TOP];
    for (int i = 0; i < KillBoard.TOP; i++) {
      tops[i] = player("Top" + (i + 1));
      kills(tops[i], 100 + i);
    }
    Player carol = player("Carol");
    kills(carol, 2);
    Player[] everyone = new Player[KillBoard.TOP + 1];
    System.arraycopy(tops, 0, everyone, 0, KillBoard.TOP);
    everyone[KillBoard.TOP] = carol;
    inWorld(world, everyone);

    killBoard.show(carol);
    killBoard.show(tops[0]);

    assertEquals(KillBoard.TOP + 1, sidebarOf(carol).size());
    assertEquals(2, sidebarOf(carol).get("Carol"));
    assertEquals(KillBoard.TOP, sidebarOf(tops[0]).size());
    assertFalse(sidebarOf(tops[0]).containsKey("Carol"));
  }

  @Test
  void refreshUpdatesCountsAndDropsLinesThatLeft() {
    World world = world();
    Player alice = player("Alice");
    Player bob = player("Bob");
    inWorld(world, alice, bob);
    kills(alice, 7);
    kills(bob, 3);
    killBoard.show(bob);

    inWorld(world, bob);
    kills(bob, 4);
    killBoard.refresh();

    assertEquals(Map.of("Bob", 4), sidebarOf(bob));
  }

  @Test
  void hidingPutsBackTheScoreboardThePlayerHadBefore() {
    World world = world();
    Player alice = player("Alice");
    inWorld(world, alice);
    killBoard.show(alice);

    assertTrue(killBoard.hide(alice));

    assertSame(mainBoard, alice.getScoreboard());
    assertFalse(killBoard.hide(alice), "hiding twice reports nothing to hide");
  }

  @Test
  void hidingLeavesAScoreboardAnotherPluginSwappedIn() {
    World world = world();
    Player alice = player("Alice");
    inWorld(world, alice);
    Scoreboard theirs = mock(Scoreboard.class);
    killBoard.show(alice);
    alice.setScoreboard(theirs);

    killBoard.hide(alice);

    assertSame(theirs, alice.getScoreboard());
  }

  @Test
  void toggleFlipsAndReportsTheNewState() {
    World world = world();
    Player alice = player("Alice");
    inWorld(world, alice);

    assertTrue(killBoard.toggle(alice));
    assertTrue(killBoard.isShowing(alice));
    assertFalse(killBoard.toggle(alice));
    assertFalse(killBoard.isShowing(alice));
    assertSame(mainBoard, alice.getScoreboard());
  }

  @Test
  void showingTwiceKeepsOneBoard() {
    World world = world();
    Player alice = player("Alice");
    inWorld(world, alice);
    killBoard.show(alice);
    Scoreboard first = alice.getScoreboard();

    assertFalse(killBoard.show(alice));
    assertSame(first, alice.getScoreboard());
  }

  @Test
  void hideAllRestoresEveryViewerAndCountsThem() {
    World world = world();
    Player alice = player("Alice");
    Player bob = player("Bob");
    inWorld(world, alice, bob);
    killBoard.show(alice);
    killBoard.show(bob);

    assertEquals(2, killBoard.hideAll());

    assertSame(mainBoard, alice.getScoreboard());
    assertSame(mainBoard, bob.getScoreboard());
    assertEquals(0, killBoard.hideAll());
  }

  @Test
  void aPlayerWhoQuitsComesBackWithItOff() {
    World world = world();
    Player alice = player("Alice");
    inWorld(world, alice);
    killBoard.show(alice);

    PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
    when(quit.getPlayer()).thenReturn(alice);
    killBoard.onQuit(quit);

    assertFalse(killBoard.isShowing(alice));
    assertEquals(0, killBoard.hideAll());
  }

  @Test
  void aQuittingPlayerDropsOffOthersBoards() {
    World world = world();
    Player alice = player("Alice");
    Player bob = player("Bob");
    inWorld(world, alice, bob);
    kills(alice, 7);
    killBoard.show(bob);

    PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
    when(quit.getPlayer()).thenReturn(alice);
    killBoard.onQuit(quit);

    assertEquals(Map.of("Bob", 0), sidebarOf(bob));
  }

  @Test
  void enableWorldShowsItToEveryoneThereNow() {
    World world = world();
    Player alice = player("Alice");
    Player bob = player("Bob");
    inWorld(world, alice, bob);

    assertEquals(2, killBoard.enableWorld(world));

    assertTrue(killBoard.isShowing(alice));
    assertTrue(killBoard.isShowing(bob));
  }

  @Test
  void aPlayerJoiningASwitchedOnWorldGetsTheBoard() {
    World world = world();
    killBoard.enableWorld(world);
    Player alice = player("Alice");
    inWorld(world, alice);

    PlayerJoinEvent join = mock(PlayerJoinEvent.class);
    when(join.getPlayer()).thenReturn(alice);
    killBoard.onJoin(join);

    assertTrue(killBoard.isShowing(alice));
  }

  @Test
  void aPlayerChangingIntoASwitchedOnWorldGetsTheBoard() {
    World arena = world();
    World nether = world();
    killBoard.enableWorld(arena);
    Player alice = player("Alice");
    inWorld(nether, alice);

    inWorld(arena, alice);
    PlayerChangedWorldEvent change = mock(PlayerChangedWorldEvent.class);
    when(change.getPlayer()).thenReturn(alice);
    killBoard.onWorldChange(change);

    assertTrue(killBoard.isShowing(alice));
  }

  @Test
  void aPlayerArrivingShowsUpOnTheBoardsAlreadyInThatWorld() {
    World arena = world();
    World nether = world();
    Player alice = player("Alice");
    Player bob = player("Bob");
    inWorld(arena, alice);
    inWorld(nether, bob);
    killBoard.show(alice);
    assertEquals(Map.of("Alice", 0), sidebarOf(alice));

    inWorld(arena, alice, bob);
    PlayerChangedWorldEvent change = mock(PlayerChangedWorldEvent.class);
    when(change.getPlayer()).thenReturn(bob);
    killBoard.onWorldChange(change);

    assertEquals(Map.of("Alice", 0, "Bob", 0), sidebarOf(alice));
  }

  @Test
  void aPlayerLeavingASwitchedOnWorldLosesTheBoard() {
    World arena = world();
    World nether = world();
    Player alice = player("Alice");
    inWorld(arena, alice);
    killBoard.enableWorld(arena);
    assertTrue(killBoard.isShowing(alice));

    inWorld(nether, alice);
    PlayerChangedWorldEvent change = mock(PlayerChangedWorldEvent.class);
    when(change.getPlayer()).thenReturn(alice);
    killBoard.onWorldChange(change);

    assertFalse(killBoard.isShowing(alice));
  }

  @Test
  void aPlayerWhoSwitchedItOnThemselvesKeepsItLeavingASwitchedOnWorld() {
    World arena = world();
    World nether = world();
    Player alice = player("Alice");
    inWorld(arena, alice);
    killBoard.show(alice);
    killBoard.enableWorld(arena);
    assertTrue(killBoard.isShowing(alice));

    inWorld(nether, alice);
    PlayerChangedWorldEvent change = mock(PlayerChangedWorldEvent.class);
    when(change.getPlayer()).thenReturn(alice);
    killBoard.onWorldChange(change);

    assertTrue(killBoard.isShowing(alice));
  }

  @Test
  void disableWorldHidesEveryoneThereIncludingSelfEnabled() {
    World world = world();
    Player alice = player("Alice");
    Player bob = player("Bob");
    inWorld(world, alice, bob);
    killBoard.show(alice);
    killBoard.enableWorld(world);
    assertTrue(killBoard.isShowing(bob));

    assertEquals(2, killBoard.disableWorld(world));

    assertFalse(killBoard.isShowing(alice));
    assertFalse(killBoard.isShowing(bob));
  }

  @Test
  void allOffClearsWorldSwitchesToo() {
    World world = world();
    killBoard.enableWorld(world);

    killBoard.hideAll();

    Player bob = player("Bob");
    inWorld(world, bob);
    PlayerJoinEvent join = mock(PlayerJoinEvent.class);
    when(join.getPlayer()).thenReturn(bob);
    killBoard.onJoin(join);

    assertFalse(killBoard.isShowing(bob));
  }
}
