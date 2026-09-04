package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckMuteCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final List<Punishment.PunishmentType> muteTypes = Arrays.asList(
            Punishment.PunishmentType.MUTE,
            Punishment.PunishmentType.TEMPMUTE,
            Punishment.PunishmentType.IPMUTE,
            Punishment.PunishmentType.TEMPIPMUTE
    );

    public CheckMuteCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.checkmute")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.checkmute.usage", ""), null));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", ""), null));
            return true;
        }

        String targetIp = null;
        if (target.isOnline() && target.getPlayer() != null && target.getPlayer().getAddress() != null) {
            targetIp = target.getPlayer().getAddress().getAddress().getHostAddress();
        }

        Punishment activeMute = dataManager.getActivePunishment(target.getUniqueId(), targetIp, muteTypes);

        if (activeMute == null || (activeMute.getDuration() != -1 && activeMute.getEnd() <= System.currentTimeMillis())) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-active-mute", ""), null));
            return true;
        }

        // Header
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.checkmute.header", ""), activeMute));

        // Details
        List<String> details = configManager.getStringList("messages.checkmute.details");
        for (String line : details) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%type%", activeMute.getType().toString());
            
            // Special handling for IP Address line
            if (line.contains("%ip_address%")) {
                if (activeMute.getIpAddress() != null && !activeMute.getIpAddress().isEmpty()) {
                    placeholders.put("%ip_address%", activeMute.getIpAddress());
                    sender.sendMessage(MessageUtil.createComponent(line, activeMute, placeholders));
                }
            } else {
                sender.sendMessage(MessageUtil.createComponent(line, activeMute, placeholders));
            }
        }

        // Footer
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.checkmute.footer", ""), activeMute));

        return true;
    }
}