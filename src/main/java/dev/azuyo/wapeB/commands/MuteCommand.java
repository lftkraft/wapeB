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

public class MuteCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;

    public MuteCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.mute")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.mute.usage", "&cUsage: /mute <player> [time] [reason] [-s] [-ip]"), null));
            return true;
        }

        String targetName = args[0];
        List<String> arguments = new ArrayList<>(Arrays.asList(args).subList(1, args.length));

        boolean silent = arguments.remove("-s");
        boolean ipMute = arguments.remove("-ip");

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

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
            return true;
        }

        String executorName = (sender instanceof Player) ? sender.getName() : configManager.getString("console-name", "Console");

        boolean success = plugin.getApi().mutePlayer(target.getUniqueId(), reason, executorName, duration, silent, ipMute);
        if (success) {
            Punishment mute = plugin.getApi().getActiveMute(target.getUniqueId());
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.mute.success", "&aSuccessfully muted %player%."), mute));
        }

        return true;
    }
}