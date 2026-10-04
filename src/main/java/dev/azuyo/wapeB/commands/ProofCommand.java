package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ProofCommand implements CommandExecutor, TabCompleter {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final List<String> subCommands = Arrays.asList("set", "remove", "reset", "check");

    public ProofCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.proof")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.proof.usage", "%prefix% &cHasználat: /proof <set|remove|reset|check> <id> [url]"), null));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "set":
                handleSet(sender, args);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            case "reset":
                handleReset(sender, args);
                break;
            case "check":
                handleCheck(sender, args);
                break;
            default:
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.proof.usage", "%prefix% &cHasználat: /proof <set|remove|reset|check> <id> [url]"), null));
                break;
        }

        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.proof.set-usage", "%prefix% &cHasználat: /proof set <id> <url>"), null));
            return;
        }

        String argId = args[1];
        String url = args[2];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int id = Integer.parseInt(argId);
                Punishment p = dataManager.getPunishment(id);

                if (p == null) {
                    sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.invalid-punishment-id", "&cÉrvénytelen büntetés ID!"), null));
                    return;
                }

                p.setProof(url);
                dataManager.savePunishment(p);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%punishment_id%", String.valueOf(p.getId()));
                placeholders.put("%proof%", url);
                placeholders.put("%player%", p.getPlayerName() != null ? p.getPlayerName() : "N/A");
                placeholders.put("%executor%", p.getExecutorName() != null ? p.getExecutorName() : "N/A");

                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.proof.set-success", "%prefix% &aBizonyíték sikeresen beállítva a(z) #%punishment_id% büntetéshez: &e%proof%"), p, placeholders));
            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.invalid-punishment-id", "&cÉrvénytelen büntetés ID!"), null));
            }
        });
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.proof.remove-usage", "%prefix% &cHasználat: /proof remove <id>"), null));
            return;
        }

        String argId = args[1];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int id = Integer.parseInt(argId);
                Punishment p = dataManager.getPunishment(id);

                if (p == null) {
                    sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.invalid-punishment-id", "&cÉrvénytelen büntetés ID!"), null));
                    return;
                }

                if (!p.hasProof()) {
                    sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.proof.no-proof", "%prefix% &cEhhez a büntetéshez nincs bizonyíték csatolva!"), p));
                    return;
                }

                p.setProof(null);
                dataManager.savePunishment(p);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%punishment_id%", String.valueOf(p.getId()));
                placeholders.put("%player%", p.getPlayerName() != null ? p.getPlayerName() : "N/A");

                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.proof.remove-success", "%prefix% &aBizonyíték sikeresen törölve a(z) #%punishment_id% büntetésből!"), p, placeholders));
            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.invalid-punishment-id", "&cÉrvénytelen büntetés ID!"), null));
            }
        });
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.proof.reset-usage", "%prefix% &cHasználat: /proof reset <id> <url>"), null));
            return;
        }

        String argId = args[1];
        String url = args[2];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int id = Integer.parseInt(argId);
                Punishment p = dataManager.getPunishment(id);

                if (p == null) {
                    sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.invalid-punishment-id", "&cÉrvénytelen büntetés ID!"), null));
                    return;
                }

                p.setProof(url);
                dataManager.savePunishment(p);

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%punishment_id%", String.valueOf(p.getId()));
                placeholders.put("%proof%", url);
                placeholders.put("%player%", p.getPlayerName() != null ? p.getPlayerName() : "N/A");
                placeholders.put("%executor%", p.getExecutorName() != null ? p.getExecutorName() : "N/A");

                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.proof.reset-success", "%prefix% &aBizonyíték sikeresen módosítva a(z) #%punishment_id% büntetésnél: &e%proof%"), p, placeholders));
            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.invalid-punishment-id", "&cÉrvénytelen büntetés ID!"), null));
            }
        });
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.proof.check-usage", "%prefix% &cHasználat: /proof check <id>"), null));
            return;
        }

        String argId = args[1];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int id = Integer.parseInt(argId);
                Punishment p = dataManager.getPunishment(id);

                if (p == null) {
                    sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.invalid-punishment-id", "&cÉrvénytelen büntetés ID!"), null));
                    return;
                }

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%punishment_id%", String.valueOf(p.getId()));
                placeholders.put("%player%", p.getPlayerName() != null ? p.getPlayerName() : "N/A");
                placeholders.put("%executor%", p.getExecutorName() != null ? p.getExecutorName() : "N/A");
                placeholders.put("%reason%", p.getReason() != null ? p.getReason() : "N/A");
                placeholders.put("%type%", p.getType().name());
                placeholders.put("%proof%", MessageUtil.formatProofPlaceholder(p));
                placeholders.put("%status%", p.isActive() 
                        ? configManager.getString("messages.proof.status-active", "&aAktív") 
                        : configManager.getString("messages.proof.status-inactive", "&cInaktív"));

                // Header
                String header = configManager.getString("messages.proof.check-header", "&8&m----------------[&c&l BIZONYÍTÉK ELLENŐRZÉS &8&m]----------------");
                if (!header.isEmpty()) {
                    sender.sendMessage(MessageUtil.createComponent(header, p, placeholders));
                }

                // Details
                List<String> details = configManager.getStringList("messages.proof.check-details");
                if (details == null || details.isEmpty()) {
                    details = Arrays.asList(
                            "&7Büntetés ID: &f#%punishment_id%",
                            "&7Játékos: &f%player%",
                            "&7Büntette: &f%executor%",
                            "&7Típus: &f%type%",
                            "&7Indok: &f%reason%",
                            "&7Állapot: %status%",
                            "&7Bizonyíték: &e%proof%"
                    );
                }

                for (String line : details) {
                    sender.sendMessage(MessageUtil.createComponent(line, p, placeholders));
                }

                // Footer
                String footer = configManager.getString("messages.proof.check-footer", "&8&m-----------------------------------------------------");
                if (!footer.isEmpty()) {
                    sender.sendMessage(MessageUtil.createComponent(footer, p, placeholders));
                }

            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.invalid-punishment-id", "&cÉrvénytelen büntetés ID!"), null));
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.proof")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return Collections.singletonList("<id>");
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("reset"))) {
            return Collections.singletonList("<url>");
        }

        return Collections.emptyList();
    }
}
