package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class WarnCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;

    public WarnCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.warn")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.warn.usage", "&cUsage: /warn <player> <reason> [-s]"), null));
            return true;
        }

        String targetNameInput = args[0];
        java.util.List<String> arguments = new java.util.ArrayList<>(Arrays.asList(args).subList(1, args.length));

        boolean silent = arguments.remove("-s");

        String proof = null;
        for (int i = arguments.size() - 1; i >= 0; i--) {
            String arg = arguments.get(i);
            if (arg.toLowerCase().startsWith("-proof:")) {
                proof = arg.substring(7);
                arguments.remove(i);
                break;
            } else if (arg.toLowerCase().startsWith("-proof=")) {
                proof = arg.substring(7);
                arguments.remove(i);
                break;
            }
        }

        String server = "global";
        for (int i = arguments.size() - 1; i >= 0; i--) {
            String arg = arguments.get(i);
            if (arg.toLowerCase().startsWith("-server:")) {
                server = arg.substring(8);
                arguments.remove(i);
                break;
            } else if (arg.toLowerCase().startsWith("-srv:")) {
                server = arg.substring(5);
                arguments.remove(i);
                break;
            } else if (arg.toLowerCase().startsWith("-server=")) {
                server = arg.substring(8);
                arguments.remove(i);
                break;
            } else if (arg.startsWith("-") && arg.length() > 1 && !arg.equalsIgnoreCase("-s") && !arg.matches("^-\\d+$")) {
                server = arg.substring(1);
                arguments.remove(i);
                break;
            }
        }

        String reasonStr = String.join(" ", arguments);

        dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate template = plugin.getTemplateManager().getTemplate("warn", reasonStr);
        if (template != null) {
            reasonStr = template.getReason();
            if (template.isSilent()) {
                silent = true;
            }
        }

        if (reasonStr.isEmpty()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.warn.usage", "&cUsage: /warn <player> <reason> [-s] [-server:<name>] [-proof:url]"), null));
            return true;
        }

        String reason = reasonStr;
        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");

        final String finalReason = reason;
        final String finalServer = server;
        final String finalProof = proof;
        final boolean finalSilent = silent;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.getApi().warnPlayer(targetNameInput, finalReason, executorName, finalSilent, finalServer, finalServer, finalProof);
            if (success) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetNameInput);
                Punishment p = new Punishment(-1, target.getUniqueId(), target.getName(), null, Punishment.PunishmentType.WARN, finalReason, executorName, finalServer, finalServer, finalProof, System.currentTimeMillis(), -1);
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.warn.success", "&aSuccessfully warned %player%."), p));
            }
        });

        return true;
    }
}