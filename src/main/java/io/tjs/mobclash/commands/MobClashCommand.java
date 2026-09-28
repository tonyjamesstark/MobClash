package io.tjs.mobclash.commands;

import io.tjs.mobclash.managers.LanguageManager;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

/**
 * {@code /mobclash <subcommand>}: every MobClash command under one name, plus {@code help}. The
 * subcommands are the executors the standalone commands use, so permissions and messages match.
 */
public class MobClashCommand implements TabExecutor {

  private final LanguageManager langManager;
  private final Map<String, BaseCommand> subcommands;

  public MobClashCommand(LanguageManager langManager, Map<String, BaseCommand> subcommands) {
    this.langManager = langManager;
    this.subcommands = subcommands;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
      sender.sendMessage(langManager.getMessage("help-header"));
      subcommands.forEach(
          (name, sub) -> {
            if (sub.canUse(sender)) {
              sender.sendMessage(langManager.getMessage("help-" + name));
            }
          });
      return true;
    }
    BaseCommand sub = subcommands.get(args[0].toLowerCase(Locale.ROOT));
    if (sub == null) {
      sender.sendMessage(langManager.getMessage("unknown-subcommand", args[0]));
      return true;
    }
    return sub.onCommand(sender, command, args[0], Arrays.copyOfRange(args, 1, args.length));
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (args.length != 1) {
      return List.of();
    }
    String typed = args[0].toLowerCase(Locale.ROOT);
    Stream<String> usable =
        subcommands.entrySet().stream()
            .filter(entry -> entry.getValue().canUse(sender))
            .map(Map.Entry::getKey);
    return Stream.concat(Stream.of("help"), usable).filter(name -> name.startsWith(typed)).toList();
  }
}
