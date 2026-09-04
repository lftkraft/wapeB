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

public class CheckBanCommand implements CommandExecutor {

    private final WapeB plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final List<Punishment.PunishmentType> banTypes = Arrays.asList(
            Punishment.PunishmentType.BAN,
            Punishment.PunishmentType.TEMPBAN,
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN
    );

    public CheckBanCommand(WapeB plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.checkban")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.checkban.usage", ""), null));
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

        Punishment activeBan = dataManager.getActivePunishment(target.getUniqueId(), targetIp, banTypes);

        if (activeBan == null || (activeBan.getDuration() != -1 && activeBan.getEnd() <= System.currentTimeMillis())) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-active-ban", ""), null));
            return true;
        }

        // Header
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.checkban.header", ""), activeBan));

        // Details
        List<String> details = configManager.getStringList("messages.checkban.details");
        for (String line : details) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%type%", activeBan.getType().toString());
            
            // Special handling for IP Address line
            if (line.contains("%ip_address%")) {
                if (activeBan.getIpAddress() != null && !activeBan.getIpAddress().isEmpty()) {
                    placeholders.put("%ip_address%", activeBan.getIpAddress());
                    sender.sendMessage(MessageUtil.createComponent(line, activeBan, placeholders));
                }
            } else {
                sender.sendMessage(MessageUtil.createComponent(line, activeBan, placeholders));
            }
        }

        // Footer
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.checkban.footer", ""), activeBan));

        return true;
    }
}