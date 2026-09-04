package dev.azuyo.wapeB.commands;

import dev.azuyo.wapeB.WapeB;
import dev.azuyo.wapeB.managers.ConfigManager;
import dev.azuyo.wapeB.managers.DataManager;
import dev.azuyo.wapeB.managers.PlayerDataManager;
import dev.azuyo.wapeB.utils.MessageUtil;
import dev.azuyo.wapeB.utils.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AltsCommand implements CommandExecutor {

    private final WapeB plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final PlayerDataManager playerDataManager;
    private final List<Punishment.PunishmentType> banTypes = Arrays.asList(
            Punishment.PunishmentType.BAN,
            Punishment.PunishmentType.TEMPBAN,
            Punishment.PunishmentType.IPBAN,
            Punishment.PunishmentType.TEMPIPBAN,
            Punishment.PunishmentType.FREEZE_LOGOUT_BAN
    );
    private final List<Punishment.PunishmentType> muteTypes = Arrays.asList(
            Punishment.PunishmentType.MUTE,
            Punishment.PunishmentType.TEMPMUTE,
            Punishment.PunishmentType.IPMUTE,
            Punishment.PunishmentType.TEMPIPMUTE
    );

    public AltsCommand(WapeB plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wapeb.alts")) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-permission", ""), null));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("alts.usage", ""), null));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.player-not-found", ""), null));
            return true;
        }

        String targetIp = playerDataManager.getLastKnownIp(target.getUniqueId());
        if (targetIp == null) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-alts-found", "").replace("%player%", target.getName()), null));
            return true;
        }

        List<UUID> alts = playerDataManager.getPlayersByIp(targetIp);
        alts.remove(target.getUniqueId()); // Remove the target player from the list

        if (alts.isEmpty()) {
            sender.sendMessage(MessageUtil.createComponent(configManager.getString("messages.no-alts-found", "").replace("%player%", target.getName()), null));
            return true;
        }

        // Header
        sender.sendMessage(MessageUtil.replacePlaceholders(configManager.getString("alts.header", ""), new Punishment(0, target.getUniqueId(), target.getName(), Punishment.PunishmentType.KICK, "", "", 0, 0)));

        // Lines
        String lineFormat = configManager.getString("alts.line", "");
        for (UUID altUuid : alts) {
            OfflinePlayer altPlayer = Bukkit.getOfflinePlayer(altUuid);
            String altPlayerName = altPlayer.getName() != null ? altPlayer.getName() : altUuid.toString();
            
            String statusColor = configManager.getString("alts.status-colors.none", "§f[CLEAN]");

            Punishment activeBan = dataManager.getActivePunishment(altUuid, null, banTypes);
            Punishment activeMute = dataManager.getActivePunishment(altUuid, null, muteTypes);

            if (activeBan != null) {
                statusColor = configManager.getString("alts.status-colors.banned", "§c[BANNED]");
            } else if (activeMute != null) {
                statusColor = configManager.getString("alts.status-colors.muted", "§e[MUTED]");
            } else if (altPlayer.isOnline()) {
                statusColor = configManager.getString("alts.status-colors.online", "§a[ONLINE]");
            } else {
                statusColor = configManager.getString("alts.status-colors.offline", "§7[OFFLINE]");
            }

            String formattedLine = lineFormat
                    .replace("%alt_player_status%", statusColor)
                    .replace("%alt_player%", altPlayerName)
                    .replace("%ip_address%", targetIp);
            sender.sendMessage(MessageUtil.createComponent(formattedLine, null));
        }

        // Footer
        sender.sendMessage(MessageUtil.replacePlaceholders(configManager.getString("alts.footer", ""), new Punishment(0, target.getUniqueId(), target.getName(), Punishment.PunishmentType.KICK, "", "", 0, 0)));

        return true;
    }
}
