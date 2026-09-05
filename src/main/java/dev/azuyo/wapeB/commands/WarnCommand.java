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

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
            return true;
        }

        boolean silent = args[args.length - 1].equalsIgnoreCase("-s");
        String reason = String.join(" ", silent ? Arrays.copyOfRange(args, 1, args.length - 1) : Arrays.copyOfRange(args, 1, args.length));

        if (reason.isEmpty()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.warn.usage", "&cUsage: /warn <player> <reason> [-s]"), null));
            return true;
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");

        boolean success = plugin.getApi().warnPlayer(target.getUniqueId(), reason, executorName, silent);
        if (success) {
            Punishment p = new Punishment(-1, target.getUniqueId(), target.getName(), Punishment.PunishmentType.WARN, reason, executorName, System.currentTimeMillis(), -1);
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.warn.success", "&aSuccessfully warned %player%."), p));
        }

        return true;
    }
}