package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.MobTracker;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SummonMobsCommandTest {

  @Mock(lenient = true)
  private MobClashPlugin plugin;

  @Mock(lenient = true)
  private SpawnManager spawnManager;

  @Mock(lenient = true)
  private LanguageManager langManager;

  @Mock(lenient = true)
  private MobTracker mobTracker;

  @Mock(lenient = true)
  private Player player;

  @Mock(lenient = true)
  private BlockCommandSender commandBlock;

  @Mock(lenient = true)
  private Command command;

  @Mock(lenient = true)
  private World world;

  @Mock(lenient = true)
  private Block block;

  @Mock(lenient = true)
  private Chest chest;

  @Mock(lenient = true)
  private Inventory inventory;

  private SummonMobsCommand summonMobsCommand;
  private Location spawnLoc;
  private Location chestLoc;

  @BeforeEach
  void setUp() {
    summonMobsCommand = new SummonMobsCommand(plugin, spawnManager, langManager);
    spawnLoc = new Location(world, 100, 64, 100);
    chestLoc = new Location(world, 50, 64, 50);

    when(plugin.getName()).thenReturn("MobClash");
    when(langManager.getMessage(anyString())).thenAnswer(invocation -> "Message");
    when(langManager.getMessage(anyString(), any())).thenAnswer(invocation -> "Message");
    when(langManager.getMessage(anyString(), any(), any())).thenAnswer(invocation -> "Message");
    when(player.hasPermission("mobspawner.summon")).thenReturn(true);
    when(player.getName()).thenReturn("TestPlayer");
    when(commandBlock.getName()).thenReturn("CommandBlock");
    doNothing().when(plugin).log(any(Level.class), anyString());
  }

  @Test
  void testSummonMobsWithValidInputRandomMode() {
    when(spawnManager.getRandom()).thenReturn(new Random(42));
    when(plugin.getMobTracker()).thenReturn(mobTracker);
    doNothing().when(mobTracker).tagMob(any(), anyString(), anyString());

    when(command.getName()).thenReturn("summonmobs");
    when(spawnManager.hasGroup("test-group")).thenReturn(true);
    when(spawnManager.getGroupChest("test-group", "wave1")).thenReturn(chestLoc);
    when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of(spawnLoc));
    when(block.getType()).thenReturn(Material.CHEST);
    when(block.getLocation()).thenReturn(chestLoc);
    when(chestLoc.getBlock()).thenReturn(block);
    when(block.getState()).thenReturn(chest);
    when(chest.getInventory()).thenReturn(inventory);

    ItemStack zombieEgg = new ItemStack(Material.ZOMBIE_SPAWN_EGG, 5);
    when(inventory.getContents()).thenReturn(new ItemStack[] {zombieEgg});

    Zombie mockZombie = mock(Zombie.class);
    when(world.spawnEntity(any(Location.class), eq(EntityType.ZOMBIE))).thenReturn(mockZombie);

    boolean result =
        summonMobsCommand.onCommand(
            player, command, "summonmobs", new String[] {"test-group", "wave1", "random", "3"});

    assertTrue(result);
    verify(world, times(3)).spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
    verify(mobTracker, times(3)).tagMob(any(), eq("test-group"), eq("wave1"));
  }

  @Test
  void testSummonMobsWithValidInputAllMode() {
    when(spawnManager.getRandom()).thenReturn(new Random(42));
    when(plugin.getMobTracker()).thenReturn(mobTracker);
    doNothing().when(mobTracker).tagMob(any(), anyString(), anyString());

    Location spawnLoc2 = new Location(world, 200, 64, 200);
    when(command.getName()).thenReturn("summonmobs");
    when(spawnManager.hasGroup("test-group")).thenReturn(true);
    when(spawnManager.getGroupChest("test-group", "wave1")).thenReturn(chestLoc);
    when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of(spawnLoc, spawnLoc2));
    when(block.getType()).thenReturn(Material.CHEST);
    when(block.getLocation()).thenReturn(chestLoc);
    when(chestLoc.getBlock()).thenReturn(block);
    when(block.getState()).thenReturn(chest);
    when(chest.getInventory()).thenReturn(inventory);

    ItemStack zombieEgg = new ItemStack(Material.ZOMBIE_SPAWN_EGG, 5);
    when(inventory.getContents()).thenReturn(new ItemStack[] {zombieEgg});

    Zombie mockZombie = mock(Zombie.class);
    when(world.spawnEntity(any(Location.class), eq(EntityType.ZOMBIE))).thenReturn(mockZombie);

    boolean result =
        summonMobsCommand.onCommand(
            player, command, "summonmobs", new String[] {"test-group", "wave1", "all", "2"});

    assertTrue(result);
    verify(world, times(4)).spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
    verify(mobTracker, times(4)).tagMob(any(), eq("test-group"), eq("wave1"));
  }

  @Test
  void testSummonMobsInvalidArguments() {
    when(command.getName()).thenReturn("summonmobs");

    boolean result =
        summonMobsCommand.onCommand(player, command, "summonmobs", new String[] {"test-group"});

    assertTrue(result);
    verify(player).sendMessage(anyString());
  }

  @Test
  void testSummonMobsGroupDoesNotExist() {
    when(command.getName()).thenReturn("summonmobs");
    when(spawnManager.hasGroup("nonexistent")).thenReturn(false);

    boolean result =
        summonMobsCommand.onCommand(
            player, command, "summonmobs", new String[] {"nonexistent", "wave1", "random"});

    assertTrue(result);
    verify(player).sendMessage(anyString());
  }

  @Test
  void testSummonMobsChestNotSet() {
    when(command.getName()).thenReturn("summonmobs");
    when(spawnManager.hasGroup("test-group")).thenReturn(true);
    when(spawnManager.getGroupChest("test-group", "wave1")).thenReturn(null);

    boolean result =
        summonMobsCommand.onCommand(
            player, command, "summonmobs", new String[] {"test-group", "wave1", "random"});

    assertTrue(result);
    verify(player).sendMessage(anyString());
  }

  @Test
  void testSummonMobsNoSpawnPoints() {
    when(command.getName()).thenReturn("summonmobs");
    when(spawnManager.hasGroup("test-group")).thenReturn(true);
    when(spawnManager.getGroupChest("test-group", "wave1")).thenReturn(chestLoc);
    when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of());

    boolean result =
        summonMobsCommand.onCommand(
            player, command, "summonmobs", new String[] {"test-group", "wave1", "random"});

    assertTrue(result);
    verify(player).sendMessage(anyString());
  }

  @Test
  void testCommandBlockCanExecute() {
    when(spawnManager.getRandom()).thenReturn(new Random(42));
    when(plugin.getMobTracker()).thenReturn(mobTracker);
    doNothing().when(mobTracker).tagMob(any(), anyString(), anyString());

    when(command.getName()).thenReturn("summonmobs");
    when(spawnManager.hasGroup("test-group")).thenReturn(true);
    when(spawnManager.getGroupChest("test-group", "wave1")).thenReturn(chestLoc);
    when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of(spawnLoc));
    when(block.getType()).thenReturn(Material.CHEST);
    when(block.getLocation()).thenReturn(chestLoc);
    when(chestLoc.getBlock()).thenReturn(block);
    when(block.getState()).thenReturn(chest);
    when(chest.getInventory()).thenReturn(inventory);

    ItemStack zombieEgg = new ItemStack(Material.ZOMBIE_SPAWN_EGG, 1);
    when(inventory.getContents()).thenReturn(new ItemStack[] {zombieEgg});

    Zombie mockZombie = mock(Zombie.class);
    when(world.spawnEntity(any(Location.class), eq(EntityType.ZOMBIE))).thenReturn(mockZombie);

    boolean result =
        summonMobsCommand.onCommand(
            commandBlock,
            command,
            "summonmobs",
            new String[] {"test-group", "wave1", "random", "1"});

    assertTrue(result);
    verify(world, times(1)).spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
    verify(mobTracker, times(1)).tagMob(any(), eq("test-group"), eq("wave1"));
  }
}
