package io.tjs.mobclash.managers;

import io.tjs.mobclash.MobClashPlugin;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

/**
 * The kill leaderboard as a sidebar, switched on per player. Each viewer gets a scoreboard of their
 * own because their own line differs from everyone else's, and the one they had before is put back
 * when it is switched off. Nothing is persisted: a player who quits rejoins with it off.
 */
public class KillBoard implements Listener {

  static final String OBJECTIVE = "mobclash_kills";
  static final int TOP = 10;

  private record Viewer(Player player, Scoreboard ours, Scoreboard previous) {}

  private final MobClashPlugin plugin;
  private final MobTracker mobTracker;
  private final LanguageManager langManager;
  private final ScoreboardManager scoreboards;
  private final Map<UUID, Viewer> viewers = new HashMap<>();

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

  /** Show the sidebar to a player. Returns false if it was already showing. */
  public boolean show(Player player) {
    if (isShowing(player)) {
      return false;
    }
    Scoreboard ours = scoreboards.getNewScoreboard();
    // The "dummy" string rather than Criteria.DUMMY, whose static initialiser needs a live server.
    ours.registerNewObjective(OBJECTIVE, "dummy", langManager.getMessage("killboard-title"))
        .setDisplaySlot(DisplaySlot.SIDEBAR);
    Viewer viewer = new Viewer(player, ours, player.getScoreboard());
    viewers.put(player.getUniqueId(), viewer);
    player.setScoreboard(ours);
    render(viewer, leaderboard());
    return true;
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

  /** Take the sidebar away from everyone. Returns how many players had it. */
  public int hideAll() {
    List<Viewer> all = List.copyOf(viewers.values());
    all.forEach(viewer -> hide(viewer.player()));
    return all.size();
  }

  /** Redraw every open sidebar. Call after anything changes a kill count. */
  public void refresh() {
    if (viewers.isEmpty()) {
      return;
    }
    Map<String, Integer> top = leaderboard();
    viewers.values().forEach(viewer -> render(viewer, top));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    viewers.remove(event.getPlayer().getUniqueId());
  }

  /** The top killers by name, resolved once per refresh rather than once per viewer. */
  private Map<String, Integer> leaderboard() {
    Map<String, Integer> top = new LinkedHashMap<>();
    for (Map.Entry<UUID, Integer> entry : mobTracker.getTopKills(TOP)) {
      String name = plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
      if (name != null) {
        top.put(name, entry.getValue());
      }
    }
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
