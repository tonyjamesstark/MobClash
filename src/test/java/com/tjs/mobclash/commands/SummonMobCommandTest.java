package com.example.mobclash.commands;

import com.example.mobclash.managers.LanguageManager;
import com.example.mobclash.managers.SpawnManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummonMobsCommandTest {

    @Mock
    private org.bukkit.plugin.java.JavaPlugin plugin;

    @Mock
    private SpawnManager spawnManager;

    @Mock
    private LanguageManager langManager;

    @Mock
    private Player player;

    @Mock
    private BlockCommandSender commandBlock;

    @Mock
    private Command command;

    @Mock
    private World world;

    @Mock
    private Block block;

    @Mock
    private Chest chest;

    @Mock
    private Inventory inventory;

    private SummonMobsCommand summonMobsCommand;
    private Location spawnLoc;
    private Location chestLoc;

    @BeforeEach
    void setUp() {
        summonMobsCommand = new SummonMobsCommand(plugin, spawnManager, langManager);
        spawnLoc = new Location(world, 100, 64, 100);
        chestLoc = new Location(world, 50, 64, 50);
        
        when(langManager.getMessage(anyString(), any())).thenReturn("Message");
        when(spawnManager.getRandom()).thenReturn(new Random(42)); // Predictable random
    }

    @Test
    void testPlayerWithPermissionCanSummonMobs() {
        when(player.hasPermission("mobspawner.summon")).thenReturn(true);
        when(spawnManager.hasGroup("test-group")).thenReturn(true);
        when(spawnManager.getGroupChest("test-group")).thenReturn(chestLoc);
        when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of(spawnLoc));
        
        when(chestLoc.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.CHEST);
        when(block.getState()).thenReturn(chest);
        when(chest.getInventory()).thenReturn(inventory);
        
        ItemStack zombieEgg = new ItemStack(Material.ZOMBIE_SPAWN_EGG, 5);
        when(inventory.getContents()).thenReturn(new ItemStack[]{zombieEgg});

        boolean result = summonMobsCommand.onCommand(player, command, "summonmobs", 
            new String[]{"test-group", "random", "1"});

        assertTrue(result);
        verify(world).spawnEntity(eq(spawnLoc), eq(EntityType.ZOMBIE));
    }

    @Test
    void testCommandBlockBypassesPermission() {
        when(spawnManager.hasGroup("test-group")).thenReturn(true);
        when(spawnManager.getGroupChest("test-group")).thenReturn(chestLoc);
        when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of(spawnLoc));
        
        when(chestLoc.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.CHEST);
        when(block.getState()).thenReturn(chest);
        when(chest.getInventory()).thenReturn(inventory);
        
        ItemStack creeperEgg = new ItemStack(Material.CREEPER_SPAWN_EGG, 3);
        when(inventory.getContents()).thenReturn(new ItemStack[]{creeperEgg});

        boolean result = summonMobsCommand.onCommand(commandBlock, command, "summonmobs", 
            new String[]{"test-group", "random"});

        assertTrue(result);
        verify(world).spawnEntity(eq(spawnLoc), eq(EntityType.CREEPER));
    }

    @Test
    void testMissingArguments() {
        when(player.hasPermission("mobspawner.summon")).thenReturn(true);
        when(langManager.getMessage("summonmobs-usage")).thenReturn("Usage message");

        boolean result = summonMobsCommand.onCommand(player, command, "summonmobs", new String[]{"test-group"});

        assertTrue(result);
        verify(player).sendMessage("Usage message");
        verify(world, never()).spawnEntity(any(), any());
    }

    @Test
    void testInvalidMode() {
        when(player.hasPermission("mobspawner.summon")).thenReturn(true);
        when(langManager.getMessage("invalid-mode")).thenReturn("Invalid mode!");

        boolean result = summonMobsCommand.onCommand(player, command, "summonmobs", 
            new String[]{"test-group", "invalid"});

        assertTrue(result);
        verify(player).sendMessage("Invalid mode!");
    }

    @Test
    void testNonExistentGroup() {
        when(player.hasPermission("mobspawner.summon")).thenReturn(true);
        when(spawnManager.hasGroup("nonexistent")).thenReturn(false);
        when(langManager.getMessage("group-not-exist", "nonexistent")).thenReturn("Group doesn't exist!");

        boolean result = summonMobsCommand.onCommand(player, command, "summonmobs", 
            new String[]{"nonexistent", "random"});

        assertTrue(result);
        verify(player).sendMessage("Group doesn't exist!");
    }

    @Test
    void testMissingChest() {
        when(player.hasPermission("mobspawner.summon")).thenReturn(true);
        when(spawnManager.hasGroup("test-group")).thenReturn(true);
        when(spawnManager.getGroupChest("test-group")).thenReturn(null);
        when(langManager.getMessage("chest-not-set", "test-group")).thenReturn("Chest not set!");

        boolean result = summonMobsCommand.onCommand(player, command, "summonmobs", 
            new String[]{"test-group", "random"});

        assertTrue(result);
        verify(player).sendMessage("Chest not set!");
    }

    @Test
    void testSummonAllMode() {
        when(player.hasPermission("mobspawner.summon")).thenReturn(true);
        when(spawnManager.hasGroup("test-group")).thenReturn(true);
        when(spawnManager.getGroupChest("test-group")).thenReturn(chestLoc);
        
        Location loc1 = new Location(world, 100, 64, 100);
        Location loc2 = new Location(world, 200, 64, 200);
        when(spawnManager.getSpawnPoints("test-group")).thenReturn(List.of(loc1, loc2));
        
        when(chestLoc.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.CHEST);
        when(block.getState()).thenReturn(chest);
        when(chest.getInventory()).thenReturn(inventory);
        
        ItemStack skeletonEgg = new ItemStack(Material.SKELETON_SPAWN_EGG, 2);
        when(inventory.getContents()).thenReturn(new ItemStack[]{skeletonEgg});

        boolean result = summonMobsCommand.onCommand(player, command, "summonmobs", 
            new String[]{"test-group", "all", "1"});

        assertTrue(result);
        // Should spawn at both locations
        verify(world, times(2)).spawnEntity(any(Location.class), eq(EntityType.SKELETON));
    }
}