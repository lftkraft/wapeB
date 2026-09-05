package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.PlayerDataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.regex.Pattern;

public class BanIpCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    public BanIpCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.banip")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.banip.usage", "&cUsage: /banip <player/ip> [time] [reason] [-s]"), null));
            return true;
        }

        String targetIdentifier = args[0];
        String targetIp = null;
        OfflinePlayer targetPlayer = null;
        String finalTargetName = targetIdentifier;

        if (IP_PATTERN.matcher(targetIdentifier).matches()) {
            targetIp = targetIdentifier;
        } else {
            targetPlayer = Bukkit.getOfflinePlayer(targetIdentifier);
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
                return true;
            }

            finalTargetName = targetPlayer.getName() != null ? targetPlayer.getName() : targetIdentifier;
            if (targetPlayer.isOnline()) {
                targetIp = targetPlayer.getPlayer().getAddress().getAddress().getHostAddress();
            } else {
                targetIp = playerDataManager.getLastKnownIp(targetPlayer.getUniqueId());
            }
        }
        
        if (targetIp == null) {
            String noIpMessage = configManager.getString("messages.no-ip-history", "&cNo recorded IP address found for offline player %player%.")
                                             .replace("%player%", finalTargetName);
            sender.sendMessage(MessageUtil.createComponent(noIpMessage, null));
            return true;
        }

        boolean silent = args.length > 1 && args[args.length - 1].equalsIgnoreCase("-s");
        String[] reasonArgs = silent ? Arrays.copyOfRange(args, 1, args.length - 1) : Arrays.copyOfRange(args, 1, args.length);
        long duration = -1;
        String reason;

        if (reasonArgs.length > 0) {
            long parsedTime = TimeUtil.parseTime(reasonArgs[0]);
            if (parsedTime != -1) {
                duration = parsedTime;
                reason = String.join(" ", Arrays.copyOfRange(reasonArgs, 1, reasonArgs.length));
            } else {
                reason = String.join(" ", reasonArgs);
            }
        } else {
            reason = configManager.getString("messages.ban.default-reason", "The Ban Hammer has spoken!");
        }
        if (reason.isEmpty()) {
            reason = configManager.getString("messages.ban.default-reason", "The Ban Hammer has spoken!");
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");

        boolean success = plugin.getApi().banPlayer(finalTargetName, reason, executorName, duration, silent, true);
        if (success) {
            Punishment ban = plugin.getApi().getActiveBan(finalTargetName);
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.banip.success", "&aSuccessfully IP-banned %player%."), ban));
        }

        return true;
    }
}
