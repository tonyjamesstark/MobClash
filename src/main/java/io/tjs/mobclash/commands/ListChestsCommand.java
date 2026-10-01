package io.tjs.mobclash.commands;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * {@code /listchests [group]} and {@code /mobclash listchests [group]}: every chest set with {@code
 * /setchest}, whether it is still a chest, and its spawn egg count: the state a summon depends on.
 */
public class ListChestsCommand extends BaseCommand {

  private record ChestEntry(String group, String name, Location location) {}

  public ListChestsCommand(
      MobClashPlugin plugin, SpawnManager spawnManager, LanguageManager langManager) {
    super(plugin, spawnManager, langManager, "mobclash.listchests", false);
  }

  @Override
  protected boolean execute(CommandSender sender, String[] args) {
    if (args.length > 1) {
      return false;
    }

    String groupFilter = args.length == 1 ? args[0] : null;
    if (groupFilter != null && !groupExists(groupFilter)) {
      sender.sendMessage(langManager.getMessage("group-not-exist", groupFilter));
      return true;
    }

    Set<String> groupNames =
        groupFilter != null ? Set.of(groupFilter) : new TreeSet<>(spawnManager.getChestGroups());

    List<ChestEntry> entries = new ArrayList<>();
    for (String group : groupNames) {
      for (Map.Entry<String, Location> wave : spawnManager.getGroupWaves(group).entrySet()) {
        entries.add(new ChestEntry(group, wave.getKey(), wave.getValue()));
      }
    }

    if (entries.isEmpty()) {
      sender.sendMessage(langManager.getMessage("no-chests"));
      return true;
    }

    entries.sort(Comparator.comparing(ChestEntry::group).thenComparing(ChestEntry::name));

    sender.sendMessage(langManager.getMessage("listchests-header", entries.size()));
    for (ChestEntry entry : entries) {
      sendEntry(sender, entry);
    }

    return true;
  }

  /** A group exists if it has spawn points, chests, or both -- a chest-only group has no points. */
  private boolean groupExists(String groupName) {
    return spawnManager.hasGroup(groupName) || spawnManager.getChestGroups().contains(groupName);
  }

  private void sendEntry(CommandSender sender, ChestEntry entry) {
    Location loc = entry.location();
    String worldName = SpawnManager.worldNameOf(loc);
    // getBlock() goes through the world, which throws once that world is unloaded.
    if (worldName == null) {
      sender.sendMessage(
          langManager.getMessage(
              "listchests-unloaded",
              entry.group(),
              entry.name(),
              Math.round(loc.getX()),
              Math.round(loc.getY()),
              Math.round(loc.getZ())));
      return;
    }
    Block block = loc.getBlock();

    if (block.getType() != Material.CHEST) {
      sender.sendMessage(
          langManager.getMessage(
              "listchests-missing",
              entry.group(),
              entry.name(),
              worldName,
              Math.round(loc.getX()),
              Math.round(loc.getY()),
              Math.round(loc.getZ()),
              block.getType()));
      return;
    }

    Inventory inventory = ((Chest) block.getState()).getInventory();
    int eggCount = 0;
    int itemCount = 0;
    for (ItemStack item : inventory.getContents()) {
      if (item == null) {
        continue;
      }
      itemCount += item.getAmount();
      if (item.getType().getKey().getKey().endsWith("_spawn_egg")) {
        eggCount += item.getAmount();
      }
    }

    sender.sendMessage(
        langManager.getMessage(
            "listchests-entry",
            entry.group(),
            entry.name(),
            worldName,
            Math.round(loc.getX()),
            Math.round(loc.getY()),
            Math.round(loc.getZ()),
            eggCount,
            itemCount));
  }
}
