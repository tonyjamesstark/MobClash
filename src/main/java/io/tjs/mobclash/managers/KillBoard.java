package io.tjs.mobclash.managers;

import io.tjs.mobclash.MobClashPlugin;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

/**
 * The kill leaderboard as a sidebar, switched on per player or per world. Each viewer gets a
 * scoreboard of their own because their own line differs from everyone else's, and the one they had
 * before is put back when it is switched off. Nothing is persisted: a player who quits rejoins with
 * it off, and a world switch does not survive a restart.
 *
 * <p>Kill counts are global (see {@link MobTracker}), but ranking is per viewer's world: each board
 * shows the top {@link #TOP} players currently in the viewer's world, including those with 0 kills,
 * plus the viewer's own line if they are outside that top.
 *
 * <p>{@link #enableWorld} switches the board on for a world itself, not just the players in it at
 * that moment: a player who later enters the world ({@link #onJoin}, {@link #onWorldChange}) gets
 * the board too, and one who leaves it loses the board again unless they had separately switched it
 * on for themselves ({@link #show}) -- tracked per viewer as {@link Source}. {@link #disableWorld}
 * is deliberately simpler: it hides the board from everyone currently in the world, including
 * anyone who had turned it on themselves, rather than leaving self-enabled viewers alone. That
 * keeps "world off" an immediate, unambiguous action instead of one whose effect on a given player
 * depends on how they came to have the board.
 */
public class KillBoard implements Listener {

  static final String OBJECTIVE = "mobclash_kills";
  static final int TOP = 10;

  /** How a viewer came to have the board, so leaving a world knows whether to take it away. */
  private enum Source {
    PLAYER,
    WORLD
  }

  private record Viewer(Player player, Scoreboard ours, Scoreboard previous, Source source) {}

  private final MobClashPlugin plugin;
  private final MobTracker mobTracker;
  private final LanguageManager langManager;
  private final ScoreboardManager scoreboards;
  private final Map<UUID, Viewer> viewers = new HashMap<>();
  private final Set<UUID> enabledWorlds = new HashSet<>();

  public KillBoard(
      MobClashPlugin plugin,
      MobTracker mobTracker,
      LanguageManager langManager,
      ScoreboardManager scoreboards) {
    this.plugin = plugin;
    this.mobTracker = mobTracker;
    this.langManager = langManager;
    this.scoreboards = scoreboards;
  }

  public boolean isShowing(Player player) {
    return viewers.containsKey(player.getUniqueId());
  }

  /**
   * Show the sidebar to a player, as if they switched it on themselves. If it is already showing
   * because the player's world is switched on, this marks it as the player's own from now on, so it
   * follows them when they leave that world. Returns false if it was already showing.
   */
  public boolean show(Player player) {
    return show(player, Source.PLAYER);
  }

  /** Take the sidebar away from a player. Returns false if it was not showing. */
  public boolean hide(Player player) {
    Viewer viewer = viewers.remove(player.getUniqueId());
    if (viewer == null) {
      return false;
    }
    // Another plugin may have swapped the scoreboard since; only undo our own swap.
    if (player.getScoreboard() == viewer.ours()) {
      player.setScoreboard(viewer.previous());
    }
    return true;
  }

  /** Flip the sidebar for a player. Returns whether it is now showing. */
  public boolean toggle(Player player) {
    return !hide(player) && show(player);
  }

  /**
   * Take the sidebar away from everyone and clear every world switch. Returns how many players had
   * it.
   */
  public int hideAll() {
    enabledWorlds.clear();
    List<Viewer> all = List.copyOf(viewers.values());
    all.forEach(viewer -> hide(viewer.player()));
    return all.size();
  }

  /**
   * Switch the board on for a world: players there now get it, and so do players who arrive later,
   * until {@link #disableWorld} turns it off. Returns how many players in the world did not already
   * have it.
   */
  public int enableWorld(World world) {
    enabledWorlds.add(world.getUID());
    int changed = 0;
    for (Player player : world.getPlayers()) {
      if (show(player, Source.WORLD)) {
        changed++;
      }
    }
    plugin.log(
        Level.INFO,
        "Kill board switched on for world " + world.getName() + " (" + changed + " player(s))");
    return changed;
  }

  /**
   * Clear the world switch and hide the board from everyone currently in the world, whether they
   * got it from the switch or switched it on themselves (see the class javadoc). Returns how many
   * players had it.
   */
  public int disableWorld(World world) {
    enabledWorlds.remove(world.getUID());
    int changed = 0;
    for (Player player : world.getPlayers()) {
      if (hide(player)) {
        changed++;
      }
    }
    plugin.log(
        Level.INFO,
        "Kill board switched off for world " + world.getName() + " (" + changed + " player(s))");
    return changed;
  }

  /** Redraw every open sidebar. Call after anything changes a kill count. */
  public void refresh() {
    refresh(Set.of());
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    enterWorld(event.getPlayer());
    refresh();
  }

  @EventHandler
  public void onWorldChange(PlayerChangedWorldEvent event) {
    Player player = event.getPlayer();
    leaveWorld(player);
    enterWorld(player);
    // Both worlds' boards change: the player drops off one and joins the other.
    refresh();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    UUID id = event.getPlayer().getUniqueId();
    viewers.remove(id);
    // Excluded explicitly rather than relying on the player already being gone from
    // World#getPlayers(): whether PlayerQuitEvent fires before or after that removal is not
    // guaranteed, so a board recomputed from the world's current players could still list them.
    refresh(Set.of(id));
  }

  private boolean show(Player player, Source source) {
    Viewer existing = viewers.get(player.getUniqueId());
    if (existing != null) {
      if (source == Source.PLAYER && existing.source() != Source.PLAYER) {
        viewers.put(
            player.getUniqueId(),
            new Viewer(existing.player(), existing.ours(), existing.previous(), Source.PLAYER));
      }
      return false;
    }
    Scoreboard ours = scoreboards.getNewScoreboard();
    // The "dummy" string rather than Criteria.DUMMY, whose static initialiser needs a live server.
    ours.registerNewObjective(OBJECTIVE, "dummy", langManager.getMessage("killboard-title"))
        .setDisplaySlot(DisplaySlot.SIDEBAR);
    Viewer viewer = new Viewer(player, ours, player.getScoreboard(), source);
    viewers.put(player.getUniqueId(), viewer);
    player.setScoreboard(ours);
    render(viewer, worldLeaderboard(player.getWorld(), Set.of()));
    return true;
  }

  /** Give the board to a player who just entered a world switched on for everyone. */
  private void enterWorld(Player player) {
    if (enabledWorlds.contains(player.getWorld().getUID()) && !isShowing(player)) {
      show(player, Source.WORLD);
    }
  }

  /** Take the board away from a player leaving a world, if that is where they got it from. */
  private void leaveWorld(Player player) {
    Viewer viewer = viewers.get(player.getUniqueId());
    if (viewer != null && viewer.source() == Source.WORLD) {
      hide(player);
    }
  }

  private void refresh(Set<UUID> exclude) {
    if (viewers.isEmpty()) {
      return;
    }
    Map<World, Map<String, Integer>> byWorld = new HashMap<>();
    for (Viewer viewer : viewers.values()) {
      World world = viewer.player().getWorld();
      Map<String, Integer> top = byWorld.computeIfAbsent(world, w -> worldLeaderboard(w, exclude));
      render(viewer, top);
    }
  }

  /**
   * The top {@link #TOP} players currently in the world by kills, descending, ties broken by name,
   * including those with 0 kills. Resolved once per world per refresh rather than once per viewer,
   * since several viewers can share a world.
   */
  private Map<String, Integer> worldLeaderboard(World world, Set<UUID> exclude) {
    Map<String, Integer> top = new LinkedHashMap<>();
    world.getPlayers().stream()
        .filter(player -> !exclude.contains(player.getUniqueId()))
        .sorted(
            Comparator.<Player>comparingInt(player -> mobTracker.getKills(player))
                .reversed()
                .thenComparing(Player::getName))
        .limit(TOP)
        .forEach(player -> top.put(player.getName(), mobTracker.getKills(player)));
    return top;
  }

  private void render(Viewer viewer, Map<String, Integer> top) {
    Map<String, Integer> lines = new HashMap<>(top);
    lines.putIfAbsent(viewer.player().getName(), mobTracker.getKills(viewer.player()));

    // Reset only the lines that left, so the sidebar does not flicker on every kill.
    Scoreboard ours = viewer.ours();
    for (String entry : List.copyOf(ours.getEntries())) {
      if (!lines.containsKey(entry)) {
        ours.resetScores(entry);
      }
    }
    Objective objective = ours.getObjective(OBJECTIVE);
    lines.forEach((name, kills) -> objective.getScore(name).setScore(kills));
  }
}
