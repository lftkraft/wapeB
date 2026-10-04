package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.importers.ImportResult;
import dev.azuyo.wapeB.importers.PunishmentImporter;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class WapebCommand implements CommandExecutor, TabCompleter {

    private final WapeB plugin;

    public WapebCommand(WapeB plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("wapeb.reload")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                    return true;
                }
                plugin.getConfigManager().reloadConfig();
                if (plugin.getTemplateManager() != null) {
                    plugin.getTemplateManager().loadTemplates();
                }
                if (plugin.getWebAPIManager() != null) {
                    plugin.getWebAPIManager().loadConfig();
                }

                // Re-initialize TimeUtil after reload
                ConfigurationSection timeSection = plugin.getConfigManager().getConfigurationSection("time-formats");
                if (timeSection != null) {
                    TimeUtil.init(
                        timeSection.getString("permanent"),
                        timeSection.getString("year"),
                        timeSection.getString("week"),
                        timeSection.getString("day"),
                        timeSection.getString("hour"),
                        timeSection.getString("minute"),
                        timeSection.getString("second")
                    );
                }

                sender.sendMessage(Component.text("wapeB configuration has been reloaded.", NamedTextColor.GREEN));
                return true;
            } else if (args[0].equalsIgnoreCase("unlink")) {
                if (!sender.hasPermission("wapeb.unlink")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                    return true;
                }
                
                plugin.getWebAPIManager().clearAllSessions();
                String message = plugin.getConfigManager().getString("web-api.messages.unlink-success", "%prefix% <green>Minden aktív webes munkamenet lezárva.");
                sender.sendMessage(MessageUtil.createComponent(message, null));
                return true;
            } else if (args[0].equalsIgnoreCase("import")) {
                if (!sender.hasPermission("wapeb.import") && !sender.hasPermission("wapeb.admin")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                    return true;
                }

                if (args.length < 2 || args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase("help")) {
                    sender.sendMessage(Component.text("--- [ wapeB Importers ] ---", NamedTextColor.GOLD));
                    for (PunishmentImporter imp : plugin.getImportManager().getRegisteredImporters()) {
                        sender.sendMessage(Component.text("• ", NamedTextColor.YELLOW)
                                .append(Component.text(imp.getName(), NamedTextColor.GREEN))
                                .append(Component.text(" - " + imp.getDescription(), NamedTextColor.GRAY)));
                    }
                    sender.sendMessage(Component.text("Usage: /wapeb import <source> [file/options]", NamedTextColor.AQUA));
                    return true;
                }

                String sourceName = args[1].toLowerCase();
                PunishmentImporter importer = plugin.getImportManager().getImporter(sourceName);
                if (importer == null) {
                    sender.sendMessage(Component.text("Unknown importer source: '" + sourceName + "'. Type '/wapeb import list' to see available sources.", NamedTextColor.RED));
                    return true;
                }

                Map<String, Object> options = new HashMap<>();
                if (args.length >= 3) {
                    options.put("file", args[2]);
                }

                sender.sendMessage(Component.text("Starting import from " + importer.getName() + "... Please wait.", NamedTextColor.YELLOW));

                importer.executeImport(options).thenAccept(result -> {
                    if (result.isSuccess()) {
                        sender.sendMessage(Component.text("✔ Import from " + result.getSourceName() + " finished in " + result.getDurationMillis() + "ms!", NamedTextColor.GREEN));
                        sender.sendMessage(Component.text("  Imported: " + result.getImportedCount() + " | Skipped: " + result.getSkippedCount() + " | Failed: " + result.getFailedCount(), NamedTextColor.GRAY));
                        for (String d : result.getDetails()) {
                            sender.sendMessage(Component.text("  " + d, NamedTextColor.DARK_GRAY));
                        }
                    } else {
                        sender.sendMessage(Component.text("✖ Import from " + result.getSourceName() + " failed!", NamedTextColor.RED));
                        for (String err : result.getErrors()) {
                            sender.sendMessage(Component.text("  Error: " + err, NamedTextColor.RED));
                        }
                    }
                });

                return true;
            }
        }

        sender.sendMessage(Component.text("wapeB Punishments v" + plugin.getDescription().getVersion() + " by Azuyo", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Usage: /wapeb <reload|unlink|import>", NamedTextColor.GRAY));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> sub = Arrays.asList("reload", "unlink", "import");
            List<String> res = new ArrayList<>();
            for (String s : sub) {
                if (s.startsWith(args[0].toLowerCase())) res.add(s);
            }
            return res;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("import")) {
            List<String> sources = new ArrayList<>();
            sources.add("list");
            for (PunishmentImporter imp : plugin.getImportManager().getRegisteredImporters()) {
                sources.add(imp.getName());
            }
            List<String> res = new ArrayList<>();
            for (String s : sources) {
                if (s.startsWith(args[1].toLowerCase())) res.add(s);
            }
            return res;
        }
        return Collections.emptyList();
    }
}