package com.example.mobclash.managers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpawnManagerTest {

    @Mock
    private JavaPlugin plugin;

    @Mock
    private FileConfiguration config;

    @Mock
    private World world;

    private SpawnManager spawnManager;

    @BeforeEach
    void setUp() {
        when(plugin.getConfig()).thenReturn(config);
        when(config.contains(anyString())).thenReturn(false);
        spawnManager = new SpawnManager(plugin);
    }

    @Test
    void testAddSpawnPoint() {
        Location location = new Location(world, 100, 64, 100);
        
        spawnManager.addSpawnPoint("test-group", location);
        
        assertTrue(spawnManager.hasGroup("test-group"));
        List<Location> points = spawnManager.getSpawnPoints("test-group");
        assertEquals(1, points.size());
        assertEquals(location, points.get(0));
    }

    @Test
    void testAddMultipleSpawnPoints() {
        Location loc1 = new Location(world, 100, 64, 100);
        Location loc2 = new Location(world, 200, 64, 200);
        
        spawnManager.addSpawnPoint("test-group", loc1);
        spawnManager.addSpawnPoint("test-group", loc2);
        
        List<Location> points = spawnManager.getSpawnPoints("test-group");
        assertEquals(2, points.size());
        assertTrue(points.contains(loc1));
        assertTrue(points.contains(loc2));
    }

    @Test
    void testRemoveNearestSpawnPoint() {
        Location loc1 = new Location(world, 100, 64, 100);
        Location loc2 = new Location(world, 200, 64, 200);
        Location playerLoc = new Location(world, 110, 64, 110);
        
        spawnManager.addSpawnPoint("test-group", loc1);
        spawnManager.addSpawnPoint("test-group", loc2);
        
        boolean removed = spawnManager.removeNearestSpawnPoint("test-group", playerLoc);
        
        assertTrue(removed);
        List<Location> points = spawnManager.getSpawnPoints("test-group");
        assertEquals(1, points.size());
        assertEquals(loc2, points.get(0));
    }

    @Test
    void testRemoveNonExistentGroup() {
        Location playerLoc = new Location(world, 100, 64, 100);
        
        boolean removed = spawnManager.removeNearestSpawnPoint("nonexistent", playerLoc);
        
        assertFalse(removed);
    }

    @Test
    void testSetAndGetGroupChest() {
        Location chestLoc = new Location(world, 50, 64, 50);
        
        spawnManager.addSpawnPoint("test-group", new Location(world, 100, 64, 100));
        spawnManager.setGroupChest("test-group", chestLoc);
        
        Location retrieved = spawnManager.getGroupChest("test-group");
        assertEquals(chestLoc, retrieved);
    }

    @Test
    void testHasGroup() {
        assertFalse(spawnManager.hasGroup("test-group"));
        
        spawnManager.addSpawnPoint("test-group", new Location(world, 100, 64, 100));
        
        assertTrue(spawnManager.hasGroup("test-group"));
    }

    @Test
    void testGetAllGroups() {
        Location loc1 = new Location(world, 100, 64, 100);
        Location loc2 = new Location(world, 200, 64, 200);
        
        spawnManager.addSpawnPoint("group1", loc1);
        spawnManager.addSpawnPoint("group2", loc2);
        
        Map<String, List<Location>> allGroups = spawnManager.getAllGroups();
        
        assertEquals(2, allGroups.size());
        assertTrue(allGroups.containsKey("group1"));
        assertTrue(allGroups.containsKey("group2"));
    }

    @Test
    void testGetRandomSpawnPoint() {
        Location loc1 = new Location(world, 100, 64, 100);
        Location loc2 = new Location(world, 200, 64, 200);
        
        spawnManager.addSpawnPoint("test-group", loc1);
        spawnManager.addSpawnPoint("test-group", loc2);
        
        Location random = spawnManager.getRandomSpawnPoint("test-group");
        
        assertNotNull(random);
        assertTrue(random.equals(loc1) || random.equals(loc2));
    }

    @Test
    void testGetRandomSpawnPointNonExistent() {
        Location random = spawnManager.getRandomSpawnPoint("nonexistent");
        
        assertNull(random);
    }

    @Test
    void testGetSpawnPointsEmptyGroup() {
        List<Location> points = spawnManager.getSpawnPoints("nonexistent");
        
        assertTrue(points.isEmpty());
    }
}