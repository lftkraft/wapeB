package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BanCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;

    public BanCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.ban")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.ban.usage", "&cUsage: /ban <player> [time] [reason] [-s] [-ip]"), null));
            return true;
        }

        String targetName = args[0];
        List<String> arguments = new ArrayList<>(Arrays.asList(args).subList(1, args.length));

        boolean silent = arguments.remove("-s");
        boolean ipBan = arguments.remove("-ip");

        long duration = -1;
        String reason;

        if (!arguments.isEmpty()) {
            long parsedTime = TimeUtil.parseTime(arguments.get(0));
            if (parsedTime != -1) {
                duration = parsedTime;
                arguments.remove(0);
            }
        }
        
        reason = String.join(" ", arguments);
        if (reason.isEmpty()) {
            reason = configManager.getString("messages.ban.default-reason", "The Ban Hammer has spoken!");
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
             sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
             return true;
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");

        boolean success = plugin.getApi().banPlayer(target.getUniqueId(), reason, executorName, duration, silent, ipBan);
        if (success) {
            Punishment ban = plugin.getApi().getActiveBan(target.getUniqueId());
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.ban.success", "&aSuccessfully banned %player%."), ban));
        }

        return true;
    }
}