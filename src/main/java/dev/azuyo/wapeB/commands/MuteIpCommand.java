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

public class MuteIpCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    public MuteIpCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.muteip")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.muteip.usage", "&cUsage: /muteip <player/ip> [time] [reason] [-s]"), null));
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
            finalTargetName = targetPlayer.getName() != null ? targetPlayer.getName() : targetIdentifier;
            if (targetPlayer.isOnline()) {
                targetIp = targetPlayer.getPlayer().getAddress().getAddress().getHostAddress();
            } else {
                targetIp = playerDataManager.getLastKnownIp(targetPlayer.getUniqueId());
            }
            if (targetIp == null && !targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
                return true;
            }
        }

        if (targetIp == null) {
            String noIpMessage = configManager.getString("messages.no-ip-history", "&cNo recorded IP address found for offline player %player%.")
                                             .replace("%player%", finalTargetName);
            sender.sendMessage(MessageUtil.createComponent(noIpMessage, null));
            return true;
        }

        java.util.List<String> arguments = new java.util.ArrayList<>(Arrays.asList(args).subList(1, args.length));
        boolean silent = arguments.remove("-s");
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
            } else if (arg.startsWith("-") && arg.length() > 1 && !arg.equalsIgnoreCase("-s") && !arg.equalsIgnoreCase("-ip") && !arg.matches("^-\\d+$")) {
                server = arg.substring(1);
                arguments.remove(i);
                break;
            }
        }

        long duration = -1;
        String reason = null;

        if (!arguments.isEmpty()) {
            String firstArg = arguments.get(0);
            if (firstArg.startsWith("$")) {
                dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate template = plugin.getTemplateManager().getTemplate("mute", firstArg);
                if (template != null) {
                    reason = template.getReason();
                    if (template.getDuration() != null && !template.getDuration().equalsIgnoreCase("perm")) {
                        duration = TimeUtil.parseTime(template.getDuration());
                    }
                    arguments.remove(0);
                }
            } else {
                long parsedTime = TimeUtil.parseTime(firstArg);
                if (parsedTime != -1) {
                    duration = parsedTime;
                    arguments.remove(0);
                }
            }
        }

        if (reason == null || reason.isEmpty()) {
            if (!arguments.isEmpty() && arguments.get(0).startsWith("$")) {
                dev.azuyo.wapeB.managers.TemplateManager.PunishmentTemplate template = plugin.getTemplateManager().getTemplate("mute", arguments.get(0));
                if (template != null) {
                    reason = template.getReason();
                    if (duration == -1 && template.getDuration() != null && !template.getDuration().equalsIgnoreCase("perm")) {
                        duration = TimeUtil.parseTime(template.getDuration());
                    }
                }
            } else {
                reason = String.join(" ", arguments);
            }
        }
        if (reason == null || reason.isEmpty()) {
            reason = configManager.getString("messages.mute.default-reason", "You have been muted.");
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");

        boolean success = plugin.getApi().mutePlayer(finalTargetName, reason, executorName, duration, silent, true, server);
        if (success) {
            Punishment mute = plugin.getApi().getActiveMute(finalTargetName);
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.muteip.success", "&aSuccessfully IP-muted %player%."), mute));
        }

        return true;
    }
}
