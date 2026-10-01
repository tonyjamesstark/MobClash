package io.tjs.mobclash.commands;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.tjs.mobclash.MobClashPlugin;
import io.tjs.mobclash.managers.KillBoard;
import io.tjs.mobclash.managers.LanguageManager;
import io.tjs.mobclash.managers.SpawnManager;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** getMessage echoes its key, so each test asserts on which message a path ends in. */
@ExtendWith(MockitoExtension.class)
class KillBoardCommandTest {

  @Mock private MobClashPlugin plugin;
  @Mock private SpawnManager spawnManager;
  @Mock private LanguageManager langManager;
  @Mock private KillBoard killBoard;
  @Mock private Player player;
  @Mock private BlockCommandSender commandBlock;
  @Mock private Block block;
  @Mock private ConsoleCommandSender console;
  @Mock private Command command;
  @Mock private World world;
  @Mock private Server server;

  private KillBoardCommand killBoardCommand;

  @BeforeEach
  void setUp() {
    killBoardCommand = new KillBoardCommand(plugin, spawnManager, langManager, killBoard);
    lenient().when(command.getName()).thenReturn("killboard");
    lenient().when(player.getName()).thenReturn("Alice");
    lenient().when(player.hasPermission("mobclash.killboard")).thenReturn(true);
    lenient().when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
    lenient().when(world.getName()).thenReturn("arena");
    lenient().when(plugin.getServer()).thenReturn(server);
    lenient().when(server.getWorlds()).thenReturn(List.of(world));
    lenient()
        .when(langManager.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private void run(CommandSender sender, String... args) {
    killBoardCommand.onCommand(sender, command, "killboard", args);
  }

  @Test
  void aPlayerTogglesTheirOwn() {
    when(killBoard.toggle(player)).thenReturn(true, false);

    run(player);
    verify(player).sendMessage("killboard-on");
    run(player);
    verify(player).sendMessage("killboard-off");
  }

  @Test
  void aPlayerSetsTheirOwnOnOrOff() {
    run(player, "on");
    verify(killBoard).show(player);
    verify(player).sendMessage("killboard-on");

    run(player, "off");
    verify(killBoard).hide(player);
    verify(player).sendMessage("killboard-off");
  }

  @Test
  void settingYourOwnIsPlayersOnly() {
    run(console, "on");
    verify(console).sendMessage("players-only");
    verifyNoInteractions(killBoard);
  }

  @Test
  void theConsoleHasNoBoardOfItsOwn() {
    run(console);
    verify(console).sendMessage("players-only");
    verifyNoInteractions(killBoard);
  }

  @Test
  void worldOnSwitchesItOnForTheSendersWorld() {
    when(player.hasPermission("mobclash.killboard.admin")).thenReturn(true);
    when(killBoard.enableWorld(world)).thenReturn(1);

    run(player, "world", "on");

    verify(killBoard).enableWorld(world);
    verify(player).sendMessage("killboard-world-on");
    verify(langManager).getMessage("killboard-world-on", 1, "arena");
  }

  @Test
  void worldOffSwitchesItOffForTheSendersWorld() {
    when(player.hasPermission("mobclash.killboard.admin")).thenReturn(true);
    when(killBoard.disableWorld(world)).thenReturn(2);

    run(player, "world", "OFF");

    verify(killBoard).disableWorld(world);
    verify(langManager).getMessage("killboard-world-off", 2, "arena");
  }

  @Test
  void aCommandBlockUsesItsOwnWorld() {
    when(commandBlock.getBlock()).thenReturn(block);
    when(block.getLocation()).thenReturn(new Location(world, 5, 64, 5));
    when(killBoard.enableWorld(world)).thenReturn(2);

    run(commandBlock, "world", "on");

    verify(killBoard).enableWorld(world);
  }

  @Test
  void theConsoleHasNoWorldToSwitch() {
    run(console, "world", "on");
    verify(console).sendMessage("no-location");
    verifyNoInteractions(killBoard);
  }

  @Test
  void theAdminFormsNeedTheAdminPermission() {
    run(player, "world", "on");
    run(player, "alloff");

    verify(player, times(2)).sendMessage("no-permission");
    verifyNoInteractions(killBoard);
  }

  @Test
  void allOffTurnsItOffEverywhere() {
    when(killBoard.hideAll()).thenReturn(3);

    run(console, "alloff");

    verify(langManager).getMessage("killboard-alloff", 3);
    verify(console).sendMessage("killboard-alloff");
  }

  @Test
  void aBadStateOrSubcommandPrintsTheUsage() {
    when(player.hasPermission("mobclash.killboard.admin")).thenReturn(true);
    killBoardCommand.setUsage("/<command> - toggle it");

    run(player, "world");
    run(player, "world", "maybe");
    run(player, "sideways");

    verify(player, times(3)).sendMessage("§e/killboard §7- toggle it");
    verify(killBoard, never()).show(any());
    verify(killBoard, never()).hide(any());
  }

  @Test
  void namedWorldFormResolvesCaseInsensitively() {
    when(player.hasPermission("mobclash.killboard.admin")).thenReturn(true);
    when(killBoard.enableWorld(world)).thenReturn(2);

    run(player, "world", "on", "ARENA");

    verify(killBoard).enableWorld(world);
    verify(player).sendMessage("killboard-world-on");
    verify(langManager).getMessage("killboard-world-on", 2, "arena");
  }

  @Test
  void theWorldShorthandNamesAWorldBeforeTheState() {
    when(player.hasPermission("mobclash.killboard.admin")).thenReturn(true);
    when(killBoard.disableWorld(world)).thenReturn(1);

    run(player, "arena", "off");

    verify(killBoard).disableWorld(world);
    verify(langManager).getMessage("killboard-world-off", 1, "arena");
  }

  @Test
  void aNamespacedKeyAlsoResolvesTheWorld() {
    // Console bypasses permission checks entirely, so the named forms work from it too.
    when(world.getName()).thenReturn("monstermash");
    when(server.getWorlds()).thenReturn(List.of());
    when(server.getWorld(NamespacedKey.fromString("minecraft:monstermash"))).thenReturn(world);
    when(killBoard.enableWorld(world)).thenReturn(1);

    run(console, "world", "on", "minecraft:monstermash");

    verify(killBoard).enableWorld(world);
  }

  @Test
  void anUnknownWorldNameIsReportedByItself() {
    run(console, "world", "on", "Nonexistent");

    verify(console).sendMessage("killboard-unknown-world");
    verify(langManager).getMessage("killboard-unknown-world", "Nonexistent");
    verifyNoInteractions(killBoard);
  }

  @Test
  void namedWorldFormsNeedTheAdminPermissionToo() {
    run(player, "world", "on", "arena");
    run(player, "arena", "off");

    verify(player, times(2)).sendMessage("no-permission");
    verifyNoInteractions(killBoard);
  }

  @Test
  void resolveWorldMatchesCaseInsensitivelyOrByNamespacedKey() {
    assertSame(world, KillBoardCommand.resolveWorld(server, "ARENA"));
    assertNull(KillBoardCommand.resolveWorld(server, "nonexistent"));
  }
}
