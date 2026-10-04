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
                    String noPerm = plugin.getConfigManager().getString("messages.no-permission", "&cYou don't have permission.");
                    sender.sendMessage(MessageUtil.createComponent(noPerm, null));
                    return true;
                }

                if (args.length < 2 || args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase("help")) {
                    String header = plugin.getConfigManager().getString("messages.import.list-header", "<gold>--- [ <yellow>wapeB Importers</yellow> ] ---</gold>");
                    sender.sendMessage(MessageUtil.createComponent(header, null));
                    for (PunishmentImporter imp : plugin.getImportManager().getRegisteredImporters()) {
                        String line = plugin.getConfigManager().getString("messages.import.list-line", "<yellow>• <green>%source%</green> <gray>- %description%</gray>")
                                .replace("%source%", imp.getName())
                                .replace("%description%", imp.getDescription());
                        sender.sendMessage(MessageUtil.createComponent(line, null));
                    }
                    String usage = plugin.getConfigManager().getString("messages.import.usage", "%prefix% <white>Használat: <red>/wapeb import <forrás> [fájl/adatbázis]");
                    sender.sendMessage(MessageUtil.createComponent(usage, null));
                    return true;
                }

                String sourceName = args[1].toLowerCase();
                PunishmentImporter importer = plugin.getImportManager().getImporter(sourceName);
                if (importer == null) {
                    List<String> srcNames = new ArrayList<>();
                    for (PunishmentImporter imp : plugin.getImportManager().getRegisteredImporters()) {
                        srcNames.add(imp.getName());
                    }
                    String unknown = plugin.getConfigManager().getString("messages.import.unknown-source", "%prefix% <red>Ismeretlen importáló forrás: <yellow>%source%<red>. Elérhető források: <white>%sources%")
                            .replace("%source%", sourceName)
                            .replace("%sources%", String.join(", ", srcNames));
                    sender.sendMessage(MessageUtil.createComponent(unknown, null));
                    return true;
                }

                Map<String, Object> options = new HashMap<>();
                if (args.length >= 3) {
                    options.put("file", args[2]);
                }

                String started = plugin.getConfigManager().getString("messages.import.started", "%prefix% <yellow>Importálás elindítva innen: <gold>%source%<yellow>... Kérlek várj!")
                        .replace("%source%", importer.getName());
                sender.sendMessage(MessageUtil.createComponent(started, null));

                importer.executeImport(options).thenAccept(result -> {
                    if (result.isSuccess()) {
                        String success = plugin.getConfigManager().getString("messages.import.success", "%prefix% <green>✔ Sikeres importálás innen: <gold>%source% <gray>(<white>%duration%ms<gray>)")
                                .replace("%source%", result.getSourceName())
                                .replace("%duration%", String.valueOf(result.getDurationMillis()));
                        sender.sendMessage(MessageUtil.createComponent(success, null));

                        String summary = plugin.getConfigManager().getString("messages.import.summary", "%prefix% <gray>Importálva: <green>%imported% <gray>| Kihagyva: <yellow>%skipped% <gray>| Sikertelen: <red>%failed%")
                                .replace("%imported%", String.valueOf(result.getImportedCount()))
                                .replace("%skipped%", String.valueOf(result.getSkippedCount()))
                                .replace("%failed%", String.valueOf(result.getFailedCount()));
                        sender.sendMessage(MessageUtil.createComponent(summary, null));

                        for (String d : result.getDetails()) {
                            sender.sendMessage(MessageUtil.createComponent("<dark_gray>  " + d, null));
                        }
                    } else {
                        String failed = plugin.getConfigManager().getString("messages.import.failed", "%prefix% <red>✖ Sikertelen importálás innen: <gold>%source%!")
                                .replace("%source%", result.getSourceName());
                        sender.sendMessage(MessageUtil.createComponent(failed, null));

                        for (String err : result.getErrors()) {
                            sender.sendMessage(MessageUtil.createComponent("<red>  Hiba: " + err, null));
                        }
                    }
                });

                return true;
            }
        }

        sender.sendMessage(MessageUtil.createComponent("<gold>wapeB Punishments v" + plugin.getDescription().getVersion() + " by Azuyo</gold>", null));
        sender.sendMessage(MessageUtil.createComponent("<gray>Használat: /wapeb <reload|unlink|import></gray>", null));
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