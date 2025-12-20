package io.tjs.mobclash.listeners;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.MobTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class MobDeathListener implements Listener {

  private final MobClashPlugin plugin;
  private final MobTracker mobTracker;

  public MobDeathListener(MobClashPlugin plugin, MobTracker mobTracker) {
    this.plugin = plugin;
    this.mobTracker = mobTracker;
  }

  @EventHandler
  public void onMobDeath(EntityDeathEvent event) {
    // Check if this was a MobClash mob
    if (!mobTracker.isMobClashMob(event.getEntity())) {
      return;
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

    // Handle loot directly to inventory if configured
    if (plugin.getConfig().getBoolean("loot-to-inventory", false)) {
      handleLootToInventory(killer, event);
    }
  }

  private void handleLootToInventory(Player player, EntityDeathEvent event) {
    List<ItemStack> drops = new ArrayList<>(event.getDrops());
    List<ItemStack> notAdded = new ArrayList<>();

    for (ItemStack drop : drops) {
      // Try to add to inventory
      if (!player.getInventory().addItem(drop).isEmpty()) {
        // Inventory full, keep in drops
        notAdded.add(drop);
      }
    }

    // Clear original drops and only leave items that didn't fit
    event.getDrops().clear();
    event.getDrops().addAll(notAdded);

    int added = drops.size() - notAdded.size();
    if (added > 0) {
      plugin.log(
          Level.INFO, "Added " + added + " items directly to " + player.getName() + "'s inventory");
    }
  }
}
