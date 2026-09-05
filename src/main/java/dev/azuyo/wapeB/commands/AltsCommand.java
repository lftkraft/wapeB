package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.PlayerDataManager;
import dev.azuyo.wapeB.utils.AltInfo;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import dev.azuyo.wapeB.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AltsCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    public AltsCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.alts")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", "&cYou don't have permission."), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.alts.usage", "&cUsage: /alts <player>"), null));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", "&cPlayer not found."), null));
            return true;
        }

        String targetName = target.getName() != null ? target.getName() : args[0];
        String targetIp = playerDataManager.getLastKnownIp(target.getUniqueId());

        List<AltInfo> alts = playerDataManager.getDetailedAlts(target.getUniqueId());

        if (alts.isEmpty()) {
            String noAltsMsg = configManager.getString("messages.no-alts-found", "&cNo alternative accounts found for %player%.")
                    .replace("%player%", targetName);
            sender.sendMessage(MessageUtil.createComponent(noAltsMsg, null));
            return true;
        }

        // Header
        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("%player%", targetName);
        headerPlaceholders.put("%ip_address%", targetIp != null ? targetIp : "N/A");
        headerPlaceholders.put("%alt_count%", String.valueOf(alts.size()));

        String header = configManager.getString("messages.alts.header", "<dark_gray><st>------------------</st> <gradient:#FF00D9:#B300FF>Alt Fiókok: %player% (%alt_count%)</gradient> <dark_gray><st>------------------");
        sender.sendMessage(MessageUtil.createComponent(header, null, headerPlaceholders));

        // Lines
        String lineFormat = configManager.getString("messages.alts.line", "<dark_gray>- %status% <color:#C34338>%alt_player%</color> %tags% <gray>(Utoljára: %last_seen%)");
        
        for (AltInfo alt : alts) {
            Map<String, String> linePlaceholders = new HashMap<>();
            linePlaceholders.put("%alt_player%", alt.getPlayerName());
            linePlaceholders.put("%ip_address%", alt.getLastIp() != null ? alt.getLastIp() : "N/A");
            
            String status;
            Punishment activePunishment = null;

            if (alt.isBanned()) {
                activePunishment = alt.getActiveBan();
                status = configManager.getString("messages.alts.status.banned", "<red><bold>[BANNED]</bold></red>");
            } else if (alt.isMuted()) {
                activePunishment = alt.getActiveMute();
                status = configManager.getString("messages.alts.status.muted", "<yellow><bold>[MUTED]</bold></yellow>");
            } else {
                OfflinePlayer op = Bukkit.getOfflinePlayer(alt.getUuid());
                if (op.isOnline()) {
                    status = configManager.getString("messages.alts.status.online", "<green>[ONLINE]</green>");
                } else {
                    status = configManager.getString("messages.alts.status.clean", "<gray>[CLEAN]</gray>");
                }
            }

            StringBuilder tags = new StringBuilder();
            if (alt.isExempt()) {
                tags.append(" ").append(configManager.getString("messages.alts.tags.exempt", "<gradient:#00FFCC:#0099FF>[🛡️ KIVÉTEL]</gradient>"));
            }
            if (alt.getMatchType() == AltInfo.MatchType.CIDR_SUBNET) {
                tags.append(" ").append(configManager.getString("messages.alts.tags.cidr", "<dark_purple>[CIDR /24]</dark_purple>"));
            }

            linePlaceholders.put("%status%", status);
            linePlaceholders.put("%tags%", tags.toString());
            linePlaceholders.put("%last_seen%", alt.getLastSeen() > 0 ? dateFormat.format(new Date(alt.getLastSeen())) : "Ismeretlen");

            String line = lineFormat;
            sender.sendMessage(MessageUtil.createComponent(line, activePunishment, linePlaceholders));
        }

        // Footer
        String footer = configManager.getString("messages.alts.footer", "<dark_gray><st>----------------------------------------------------");
        sender.sendMessage(MessageUtil.createComponent(footer, null, headerPlaceholders));

        return true;
    }
}
