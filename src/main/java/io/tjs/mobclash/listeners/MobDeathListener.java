package io.tjs.mobclash.listeners;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.KillBoard;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class MobDeathListener implements Listener {

  private final MobClashPlugin plugin;
  private final MobTracker mobTracker;
  private final KillBoard killBoard;
  private final LanguageManager langManager;

  public MobDeathListener(
      MobClashPlugin plugin,
      MobTracker mobTracker,
      KillBoard killBoard,
      LanguageManager langManager) {
    this.plugin = plugin;
    this.mobTracker = mobTracker;
    this.killBoard = killBoard;
    this.langManager = langManager;
  }

  /**
   * HIGHEST so the drops are final before they move to the killer, and ignoreCancelled because a
   * cancelled death on Paper revives the mob: it drops nothing and is not a kill.
   */
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onMobDeath(EntityDeathEvent event) {
    // Check if this was a MobClash mob
    if (!mobTracker.isMobClashMob(event.getEntity())) {
      return;
    }

    // The wither drops its nether star in code, past any DeathLootTable the egg sets, and keeps
    // it from despawning for ten minutes. A summoned wither drops none, whoever kills it.
    if (event.getEntityType() == EntityType.WITHER) {
      event.getDrops().removeIf(drop -> drop.getType() == Material.NETHER_STAR);
    }

    // Check if killed by a player
    if (!(event.getEntity().getKiller() instanceof Player)) {
      return;
    }

    Player killer = event.getEntity().getKiller();
    String spawnInfo = mobTracker.getMobSpawnInfo(event.getEntity());

    plugin.log(
        Level.INFO,
        killer.getName()
            + " killed MobClash mob: "
            + event.getEntityType()
            + " ("
            + spawnInfo
            + ")");

    // Record the kill
    mobTracker.recordKill(killer);
    killBoard.refresh();
    announceIfListed(killer, event);

    // The killer is whoever hit the mob last, who may have logged out or died before it fell.
    // Their inventory is then no place for the loot, so it stays on the ground.
    if (plugin.getConfig().getBoolean("loot-to-inventory", false)
        && killer.isOnline()
        && !killer.isDead()) {
      handleLootToInventory(killer, event);
    }
  }

  /**
   * Tell every player in every world, and the console, when the mob is one announce-kills lists.
   * Read at each kill so /mobclash reload applies an edit.
   */
  private void announceIfListed(Player killer, EntityDeathEvent event) {
    NamespacedKey type = event.getEntityType().getKey();
    boolean listed =
        plugin.getConfig().getStringList("announce-kills").stream()
            .anyMatch(name -> type.equals(NamespacedKey.fromString(name.toLowerCase(Locale.ROOT))));
    if (listed) {
      String message =
          langManager.getMessage(
              "kill-announcement", killer.getName(), event.getEntity().getName());
      plugin.getServer().broadcast(LegacyComponentSerializer.legacySection().deserialize(message));
    }
  }

  private void handleLootToInventory(Player player, EntityDeathEvent event) {
    List<ItemStack> drops = new ArrayList<>(event.getDrops());
    List<ItemStack> notAdded = new ArrayList<>();

    int offered = 0;
    for (ItemStack drop : drops) {
      offered += drop.getAmount();
      // addItem returns what did not fit. The previous code re-added the original stack instead,
      // which was correct only because CraftBukkit happens to decrement it in place; an
      // implementation that copied would have duplicated the items.
      notAdded.addAll(player.getInventory().addItem(drop).values());
    }

    // Clear original drops and only leave items that didn't fit
    event.getDrops().clear();
    event.getDrops().addAll(notAdded);

    int leftOver = notAdded.stream().mapToInt(ItemStack::getAmount).sum();
    int added = offered - leftOver;
    if (added > 0) {
      plugin.log(
          Level.INFO, "Added " + added + " items directly to " + player.getName() + "'s inventory");
    }
  }
}
