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
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
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
 * assert on what a player would read rather than on which calls produced it.
 */
@ExtendWith(MockitoExtension.class)
class KillBoardTest {

  @Mock private MobClashPlugin plugin;
  @Mock private Server server;
  @Mock private MobTracker mobTracker;
  @Mock private LanguageManager langManager;
  @Mock private ScoreboardManager scoreboards;
  @Mock private Scoreboard mainBoard;

  private final Map<Scoreboard, Map<String, Integer>> sidebars = new HashMap<>();
  private final Map<String, UUID> ids = new HashMap<>();
  private KillBoard killBoard;

  @BeforeEach
  void setUp() {
    killBoard = new KillBoard(plugin, mobTracker, langManager, scoreboards);
    lenient().when(plugin.getServer()).thenReturn(server);
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

  /** A player whose scoreboard behaves like the real one: set replaces what get returns. */
  private Player player(String name) {
    Player player = mock(Player.class);
    UUID id = ids.computeIfAbsent(name, n -> UUID.randomUUID());
    AtomicReference<Scoreboard> current = new AtomicReference<>(mainBoard);
    lenient().when(player.getUniqueId()).thenReturn(id);
    lenient().when(player.getName()).thenReturn(name);
    lenient().when(player.getScoreboard()).thenAnswer(invocation -> current.get());
    lenient()
        .doAnswer(invocation -> current.getAndSet(invocation.getArgument(0)))
        .when(player)
        .setScoreboard(any());
    return player;
  }

  /** The kill totals, top first, as MobTracker reports them, plus each player's own count. */
  private void givenKills(Map<String, Integer> kills) {
    List<Map.Entry<UUID, Integer>> top =
        kills.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(KillBoard.TOP)
            .map(
                e ->
                    Map.entry(
                        ids.computeIfAbsent(e.getKey(), n -> UUID.randomUUID()), e.getValue()))
            .toList();
    lenient().when(mobTracker.getTopKills(KillBoard.TOP)).thenReturn(top);
    kills.forEach(
        (name, count) -> {
          OfflinePlayer offline = mock(OfflinePlayer.class);
          lenient().when(offline.getName()).thenReturn(name);
          lenient().when(server.getOfflinePlayer(ids.get(name))).thenReturn(offline);
        });
    lenient()
        .when(mobTracker.getKills(any(Player.class)))
        .thenAnswer(
            invocation -> kills.getOrDefault(invocation.<Player>getArgument(0).getName(), 0));
  }

  private Map<String, Integer> sidebarOf(Player player) {
    return sidebars.get(player.getScoreboard());
  }

  @Test
  void showingPutsTheLeaderboardInTheSidebar() {
    givenKills(Map.of("Alice", 7, "Bob", 3));
    Player alice = player("Alice");

    assertTrue(killBoard.show(alice));

    assertNotSame(mainBoard, alice.getScoreboard());
    assertEquals(Map.of("Alice", 7, "Bob", 3), sidebarOf(alice));
    Objective objective = alice.getScoreboard().getObjective(KillBoard.OBJECTIVE);
    verify(objective).setDisplaySlot(DisplaySlot.SIDEBAR);
  }

  @Test
  void aViewerOutsideTheTopTenStillSeesTheirOwnLine() {
    Map<String, Integer> kills = new HashMap<>();
    for (int i = 1; i <= KillBoard.TOP; i++) {
      kills.put("Top" + i, 100 + i);
    }
    kills.put("Carol", 2);
    givenKills(kills);
    Player carol = player("Carol");
    Player top1 = player("Top1");

    killBoard.show(carol);
    killBoard.show(top1);

    assertEquals(KillBoard.TOP + 1, sidebarOf(carol).size());
    assertEquals(2, sidebarOf(carol).get("Carol"));
    assertEquals(KillBoard.TOP, sidebarOf(top1).size());
    assertFalse(sidebarOf(top1).containsKey("Carol"));
  }

  @Test
  void aNewViewerWithNoKillsSeesAZero() {
    givenKills(Map.of("Alice", 7));
    Player dave = player("Dave");

    killBoard.show(dave);

    assertEquals(Map.of("Alice", 7, "Dave", 0), sidebarOf(dave));
  }

  @Test
  void refreshUpdatesCountsAndDropsLinesThatLeft() {
    givenKills(Map.of("Alice", 7, "Bob", 3));
    Player bob = player("Bob");
    killBoard.show(bob);

    givenKills(Map.of("Bob", 4));
    killBoard.refresh();

    assertEquals(Map.of("Bob", 4), sidebarOf(bob));
  }

  @Test
  void hidingPutsBackTheScoreboardThePlayerHadBefore() {
    givenKills(Map.of());
    Player alice = player("Alice");
    killBoard.show(alice);

    assertTrue(killBoard.hide(alice));

    assertSame(mainBoard, alice.getScoreboard());
    assertFalse(killBoard.hide(alice), "hiding twice reports nothing to hide");
  }

  @Test
  void hidingLeavesAScoreboardAnotherPluginSwappedIn() {
    givenKills(Map.of());
    Player alice = player("Alice");
    Scoreboard theirs = mock(Scoreboard.class);
    killBoard.show(alice);
    alice.setScoreboard(theirs);

    killBoard.hide(alice);

    assertSame(theirs, alice.getScoreboard());
  }

  @Test
  void toggleFlipsAndReportsTheNewState() {
    givenKills(Map.of());
    Player alice = player("Alice");

    assertTrue(killBoard.toggle(alice));
    assertTrue(killBoard.isShowing(alice));
    assertFalse(killBoard.toggle(alice));
    assertFalse(killBoard.isShowing(alice));
    assertSame(mainBoard, alice.getScoreboard());
  }

  @Test
  void showingTwiceKeepsOneBoard() {
    givenKills(Map.of());
    Player alice = player("Alice");
    killBoard.show(alice);
    Scoreboard first = alice.getScoreboard();

    assertFalse(killBoard.show(alice));
    assertSame(first, alice.getScoreboard());
  }

  @Test
  void hideAllRestoresEveryViewerAndCountsThem() {
    givenKills(Map.of());
    Player alice = player("Alice");
    Player bob = player("Bob");
    killBoard.show(alice);
    killBoard.show(bob);

    assertEquals(2, killBoard.hideAll());

    assertSame(mainBoard, alice.getScoreboard());
    assertSame(mainBoard, bob.getScoreboard());
    assertEquals(0, killBoard.hideAll());
  }

  @Test
  void aPlayerWhoQuitsComesBackWithItOff() {
    givenKills(Map.of());
    Player alice = player("Alice");
    killBoard.show(alice);

    PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
    when(quit.getPlayer()).thenReturn(alice);
    killBoard.onQuit(quit);

    assertFalse(killBoard.isShowing(alice));
    assertEquals(0, killBoard.hideAll());
  }
}
