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

        Punishment activeBan = plugin.getApi().getActiveBan(target.getUniqueId());

        if (activeBan == null || (activeBan.getDuration() != -1 && activeBan.getEnd() <= System.currentTimeMillis())) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-active-ban", ""), null));
            return true;
        }

        Map<String, String> headerPlaceholders = new HashMap<>();
        String targetName = target.getName() != null ? target.getName() : args[0];
        headerPlaceholders.put("%target%", targetName);
        boolean isAltBan = activeBan.getPlayerUuid() != null && !activeBan.getPlayerUuid().equals(target.getUniqueId());
        headerPlaceholders.put("%alt_notice%", isAltBan ? " (Alt fiók: " + (activeBan.getPlayerName() != null ? activeBan.getPlayerName() : "Ismeretlen") + ")" : "");
        headerPlaceholders.put("%punished_player%", activeBan.getPlayerName() != null ? activeBan.getPlayerName() : targetName);

        // Header
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.checkban.header", ""), activeBan, headerPlaceholders));

        // Details
        List<String> details = configManager.getStringList("messages.checkban.details");
        for (String line : details) {
            Map<String, String> placeholders = new HashMap<>(headerPlaceholders);
            placeholders.put("%type%", activeBan.getType().toString());
            
            if (activeBan.getIpAddress() != null && !activeBan.getIpAddress().isEmpty()) {
                placeholders.put("%ip_address%", activeBan.getIpAddress());
                placeholders.put("%geoip%", dev.azuyo.wapeB.utils.GeoIPUtil.getGeoInfo(activeBan.getIpAddress()).getFormatted());
            }

            if (line.contains("%ip_address%") && (activeBan.getIpAddress() == null || activeBan.getIpAddress().isEmpty())) {
                continue;
            }

            sender.sendMessage(MessageUtil.createComponent(line, activeBan, placeholders));
        }

        // Footer
        sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.checkban.footer", ""), activeBan, headerPlaceholders));

        return true;
    }
}